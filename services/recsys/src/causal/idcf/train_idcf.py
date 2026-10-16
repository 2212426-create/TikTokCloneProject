# -*- coding: utf-8 -*-
"""
Huấn luyện iDCF hai giai đoạn theo mã tác giả (github.com/BgmLover/iDCF):
  1. ExposureIVAE trên exposure train và đặc trưng user (proxy W) -> mean/std C.
  2. IDCFModel với MSE trên điểm thô; chọn epoch bằng validation NDCG@5, lưu checkpoint,
     chấm test một lần qua evaluate_checkpoint.
Đầu vào theo dataset:
  - Coat: exposure = train.ascii > 0; W = Z = user_features.ascii (14 cột one-hot).
  - KuaiRand: exposure = mọi cặp (user, video) trong standard log đã lọc (cả click lẫn không);
    W = đặc trưng user one-hot như notebook tác giả; Z = tỷ lệ exposure theo tab.
Project: ViFarm (services/recsys)
"""

import hashlib
import os

import numpy as np
import torch
import torch.nn.functional as F
from scipy import sparse

from src.causal.idcf.idcf_model import IDCFModel
from src.causal.idcf.ivae import train_exposure_ivae
from src.causal.idcf.ividr_staged import DEFAULT_ALPHA_RESIDUAL, ividr_confounders
from src.data_pipeline.coat_dataset import load_coat_processed, load_coat_raw_features
from src.data_pipeline.kuairand_dataset import load_kuairand_author_proxy, load_kuairand_tab_instruments
from src.evaluation.evaluator import evaluate_unbiased_model
from src.evaluation.protocol import TOP_K
from src.utils.artifacts import (checkpoint_path, evaluate_checkpoint, make_metadata, save_checkpoint,
                                 validate_training)
from src.utils.helpers import CHECKPOINT_DIR, set_seed


def coat_inputs(data):
    raw = load_coat_raw_features(data.get("data_dir"))
    return {"exposure": raw["train_matrix"] > 0, "proxy": raw["user_features"],
            "instrument": raw["user_features"]}


def kuairand_inputs(data):
    n = len(data["train_u"])
    exposure = sparse.csr_matrix((np.ones(n, dtype=np.float32), (data["train_u"], data["train_i"])),
                                 shape=(data["n_users"], data["n_items"]))
    instrument, _ = load_kuairand_tab_instruments(data["user_mapping"], data["video_mapping"], data.get("data_dir"))
    return {"exposure": exposure, "proxy": load_kuairand_author_proxy(data["user_mapping"], data.get("data_dir")),
            "instrument": instrument}


DATASET_INPUTS = {"coat": coat_inputs, "kuairand": kuairand_inputs}

# Giai đoạn 2 theo iDCF.py: lr 5e-4, wd 1e-6, dim 64, ≤ 100 epoch; KuaiRand lr 1e-4 (nhánh chạy nhiều seed
# của tác giả) và batch 2048 (argparser.py). latent 16 và patience 20 là lựa chọn địa phương.
IDCF_CONFIG = {
    "coat": {"ivae": dict(latent_dim=16, batch_size=512),
             "train": dict(lr=5e-4, weight_decay=1e-6, embedding_dim=64, batch_size=512, patience=20)},
    "kuairand": {"ivae": dict(latent_dim=16, batch_size=2048),
                 "train": dict(lr=1e-4, weight_decay=1e-6, embedding_dim=64, batch_size=2048, patience=5)},
}


def idcf_confounders(inputs, seed=42, latent_dim=16, **ivae_kwargs):
    """Giai đoạn 1: C ~ q(C | A, W) từ iVAE trên exposure."""
    return train_exposure_ivae(inputs["exposure"], inputs["proxy"], latent_dim=latent_dim, seed=seed, **ivae_kwargs)


def train_idcf(data, confounder_mean, confounder_std, dataset_name="coat", seed=42, lr=5e-4, weight_decay=1e-6,
               embedding_dim=64, batch_size=512, epochs=100, patience=20, evaluate_test=True,
               checkpoint_dir=None, checkpoint_tag="manual", extra_config=None):
    validate_training(lr=lr, embedding_dim=embedding_dim, batch_size=batch_size, epochs=epochs, patience=patience)
    mean = torch.as_tensor(confounder_mean, dtype=torch.float32)
    std = torch.as_tensor(confounder_std, dtype=torch.float32)
    set_seed(seed)
    model = IDCFModel(data["n_users"], data["n_items"], mean.shape[1], embedding_dim=embedding_dim)
    model.set_confounders(mean, std)
    optimizer = torch.optim.Adam(model.parameters(), lr=lr, weight_decay=weight_decay)
    confounders = hashlib.sha256(mean.numpy().tobytes() + std.numpy().tobytes()).hexdigest()[:16]
    metadata = make_metadata(
        "idcf", dataset_name, data,
        dict(lr=lr, weight_decay=weight_decay, batch_size=batch_size, epochs=epochs, patience=patience,
             seed=seed, loss="mse", confounders_sha256=confounders, **(extra_config or {})),
        dict(num_users=data["n_users"], num_items=data["n_items"], latent_dim=mean.shape[1],
             embedding_dim=embedding_dim))
    save_path = checkpoint_path(checkpoint_dir or os.path.join(CHECKPOINT_DIR, "idcf"),
                                f"{dataset_name}_idcf_{checkpoint_tag}", metadata)
    u = torch.as_tensor(data["train_u"], dtype=torch.long)
    i = torch.as_tensor(data["train_i"], dtype=torch.long)
    y = torch.as_tensor(data["train_y"], dtype=torch.float32)
    include = data.get("evaluation_include_zero_positive_users", True)
    best = {"ndcg": -1.0, "epoch": 0}
    for epoch in range(1, epochs + 1):
        model.train()
        for idx in torch.randperm(len(y)).split(batch_size):
            loss = F.mse_loss(model(u[idx], i[idx], sample=True), y[idx])
            optimizer.zero_grad()
            loss.backward()
            optimizer.step()
        model.eval()
        val = evaluate_unbiased_model(model, data["val_candidates"], data["val_ground_truth"], k=TOP_K,
                                      include_zero_positive_users=include)
        if val["NDCG@5"] > best["ndcg"]:
            best.update(ndcg=val["NDCG@5"], epoch=epoch)
            save_checkpoint(model, save_path, metadata, epoch, val)
        elif epoch - best["epoch"] >= patience:
            break
    if not evaluate_test:
        return {"best_val_ndcg": best["ndcg"], "best_epoch": best["epoch"], "epochs_run": epoch,
                "checkpoint_path": save_path, "metadata": metadata}
    return {**evaluate_checkpoint(save_path, data), "epochs_run": epoch}


def fit_idcf_pipeline(data, dataset_name, seed=42, item_emb=None, alpha_residual=DEFAULT_ALPHA_RESIDUAL,
                      checkpoint_dir=None, checkpoint_tag="manual", evaluate_test=True, epochs=100,
                      ivae_epochs=2000):
    """iDCF khi item_emb là None; ngược lại IViDR theo giai đoạn với treatment T = item_emb."""
    config = IDCF_CONFIG[dataset_name]
    inputs = DATASET_INPUTS[dataset_name](data)
    if item_emb is None:
        mean, std, info = idcf_confounders(inputs, seed=seed, epochs=ivae_epochs, **config["ivae"])
        extra = {"variant": "idcf"}
    else:
        mean, std, info = ividr_confounders(inputs, item_emb, seed=seed, alpha_residual=alpha_residual,
                                            epochs=ivae_epochs, **config["ivae"])
        extra = {"variant": "ividr_staged", "alpha_residual": alpha_residual,
                 "first_stage_r2": info["first_stage_r2"]}
    result = train_idcf(data, mean, std, dataset_name, seed=seed, epochs=epochs, evaluate_test=evaluate_test,
                        checkpoint_dir=checkpoint_dir, checkpoint_tag=checkpoint_tag, extra_config=extra,
                        **config["train"])
    return {**result, "confounder_info": info}


if __name__ == "__main__":
    r = fit_idcf_pipeline(load_coat_processed(), "coat")
    print({k: r[k] for k in ("NDCG@5", "Recall@5", "best_epoch", "checkpoint_path")})
