# -*- coding: utf-8 -*-
"""
Hàm mất mát DR-BIAS và Bộ lọc Thận trọng CDR (Conservative Doubly Robust)
Biến thể địa phương tham khảo CDR (Song et al.) và DR-BIAS (Dai et al.)
Project: ViFarm (services/recsys)
"""

import torch
import torch.nn as nn
import torch.nn.functional as F


def dr_bias_closed_form_imputation_loss(e_loss, e_hat_loss, inv_prop):
    """
    Hàm mất mát huấn luyện mô hình Imputation Model theo DR-BIAS (Dai et al., 2022; Song et al., CIKM 2023):
      L_imp = sum_{O} (e_ui - e_hat_ui)^2 * (inv_p)^3 * (1 - 1/inv_p)^2
    giúp tối ưu hóa cân bằng giữa bias và variance của bộ gán nhãn sai số.
    """
    weight = (inv_prop ** 3) * ((1.0 - 1.0 / torch.clamp(inv_prop, min=1.0)) ** 2)
    imp_loss = (((e_loss - e_hat_loss) ** 2) * weight).mean()
    return imp_loss


def cdr_recommendation_loss(pred_obs, y_obs, inv_prop_obs, imp_loss_obs, imp_unc_obs,
                            pred_unobs, imp_loss_unobs, imp_unc_unobs,
                            un_thres=10.0, counterfactual_scale=1.0,
                            filter_mode="variance", ips_weighting="author"):
    """
    Hàm mất mát Conservative Doubly Robust (CDR) cập nhật mô hình gợi ý chính:
      L_CDR = (IPS_part + Direct_part) / |x_unobs|
    trong đó:
      - IPS_part = sum_{O} [ inv_p * e_ui - (inv_p - 1) * e_hat_ui * I(sigma / mu < un_thres) ]
      - Direct_part = sum_{D} e_hat_ui * I(sigma / mu < un_thres)
      - Bộ lọc CDR loại bỏ poisonous imputation theo hệ số biến thiên (Coefficient of Variation)
        dùng trong biến thể địa phương: sigma / mu < eta
    filter_mode="variance" (mặc định): lọc sigma^2 < un_thres như mã tác giả (CDR/MF_mcdropout.py);
      "cv" (cũ): sigma / mu < un_thres; với un_thres = 0.01 lọc bỏ ~99% imputation trên Coat.
    ips_weighting="author" (mặc định): trừ e_hat * filter không trọng số như mã tác giả;
      "dr" (cũ): trừ (inv_p - 1) * e_hat.
    """
    if filter_mode not in ("cv", "variance") or ips_weighting not in ("dr", "author"):
        raise ValueError("filter_mode must be cv/variance and ips_weighting dr/author")

    def keep(loss, unc):
        if filter_mode == "variance":
            return (unc < un_thres).float()
        return ((torch.sqrt(unc.clamp(min=1e-8)) / loss.detach().clamp(min=1e-4)) < un_thres).float()

    # 1. Thành phần trên tập quan sát (IPS có hiệu chỉnh imputation đã lọc)
    xent_loss = (F.binary_cross_entropy(pred_obs, y_obs, reduction="none") * inv_prop_obs).sum()
    
    filter_obs = keep(imp_loss_obs, imp_unc_obs)
    # "dr": trừ (inv_prop - 1) * e_hat; "author": trừ e_hat không trọng số.
    prop_weight_obs = (torch.clamp(inv_prop_obs - 1.0, min=0.0) if ips_weighting == "dr"
                       else torch.ones_like(inv_prop_obs))
    imputation_loss_obs = (imp_loss_obs * filter_obs * prop_weight_obs).sum()
    ips_loss = xent_loss - imputation_loss_obs

    # 2. Thành phần trên tập đối chứng / chưa quan sát (Direct Imputation đã lọc)
    filter_unobs = keep(imp_loss_unobs, imp_unc_unobs)
    direct_loss = (imp_loss_unobs * filter_unobs).sum() * counterfactual_scale

    num_samples = max(pred_unobs.shape[0], 1)
    loss = (ips_loss + direct_loss) / num_samples

    total_filtered = (1.0 - filter_unobs).sum().item() + (1.0 - filter_obs).sum().item()
    total_items = filter_unobs.numel() + filter_obs.numel()
    filtered_ratio = total_filtered / max(total_items, 1)

    return loss, filtered_ratio

