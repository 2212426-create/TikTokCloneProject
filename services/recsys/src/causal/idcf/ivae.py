# -*- coding: utf-8 -*-
"""
Giai đoạn 1 của iDCF (Zhang et al., KDD 2023): iVAE học biến ẩn C_u từ vector exposure A_u.
Port từ github.com/BgmLover/iDCF (models/vae.py::iVAE, ivae_exposure.py, save_ae_params.ipynb):
  - Đầu vào encoder: [A_u ∈ {0,1}^n_items, W_u]; prior p(C|W) = N(0, exp(MLP(W))).
  - Decoder tái tạo A_u bằng BCE-with-logits cộng trên item; KL anneal tuyến tính tới beta_max
    trong 20% số bước đầu; phạt thêm l2_penalty * sum ||θ||.
  - Chia user 80/20 (seed 1234) để chọn mô hình theo val loss; mean/std lưu cho MỌI user.
Exposure có thể là ma trận thưa (KuaiRand); chỉ từng batch được chuyển sang dense.
Project: ViFarm (services/recsys)
"""

import copy

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F
from scipy import sparse


def mlp(input_dim, hidden_dim, output_dim, n_layers):
    layers, prev = [], input_dim
    for _ in range(n_layers - 1):
        layers += [nn.Linear(prev, hidden_dim), nn.LeakyReLU(0.2)]
        prev = hidden_dim
    layers.append(nn.Linear(prev, output_dim))
    return nn.Sequential(*layers)


class ExposureIVAE(nn.Module):
    def __init__(self, n_items, aux_dim, latent_dim=32, hidden_dim=64, n_layers=3, extra_dim=0):
        super().__init__()
        self.prior_log_var = mlp(aux_dim, hidden_dim, latent_dim, n_layers)
        # extra_dim > 0: encoder nhận thêm đặc trưng (IViDR: treatment tái tạo), decoder vẫn tái tạo A.
        self.mean_z = mlp(n_items + extra_dim + aux_dim, hidden_dim, latent_dim, n_layers)
        self.log_var_z = mlp(n_items + extra_dim + aux_dim, hidden_dim, latent_dim, n_layers)
        self.decoder = mlp(latent_dim, hidden_dim, n_items, n_layers)

    def encode(self, a, w):
        aw = torch.cat([a, w], 1)
        return self.mean_z(aw), self.log_var_z(aw)

    def forward(self, x, w):
        """x = A (iDCF) hoặc [A, đặc trưng phụ]; mục tiêu tái tạo luôn là A."""
        prior_log_var = self.prior_log_var(w)
        mean, log_var = self.encode(x, w)
        z = mean + torch.exp(0.5 * log_var) * torch.randn_like(mean)
        return self.decoder(z), mean, log_var, torch.zeros_like(prior_log_var), prior_log_var


def elbo_loss(a, a_hat, mean, log_var, prior_mean, prior_log_var, anneal=1.0):
    recon = F.binary_cross_entropy_with_logits(a_hat, a, reduction="none").sum(1).mean()
    kld = -0.5 * torch.sum(1 + log_var - prior_log_var
                           - ((mean - prior_mean).pow(2) + log_var.exp()) / prior_log_var.exp(), 1).mean()
    return recon + kld * anneal


def binary_exposure(exposure):
    """Ma trận exposure 0/1 (float32), giữ dạng thưa CSR nếu đầu vào thưa."""
    if sparse.issparse(exposure):
        return (sparse.csr_matrix(exposure) > 0).astype(np.float32)
    return (np.asarray(exposure) > 0).astype(np.float32)


def train_exposure_ivae(exposure, user_features, latent_dim=32, hidden_dim=64, n_layers=3, lr=5e-4,
                        weight_decay=1e-6, l2_penalty=0.01, batch_size=512, epochs=2000, patience=100,
                        beta_max=1.0, train_ratio=0.8, split_seed=1234, seed=42, verbose=False,
                        extra_input=None):
    """Trả về (mean, std) của q(C | A, W) cho mọi user, từ mô hình có val loss tốt nhất.
    extra_input (n_users × d) được nối vào đầu vào encoder (nhánh C1 của IViDR)."""
    a_all = binary_exposure(exposure)
    w_all = torch.as_tensor(np.asarray(user_features), dtype=torch.float32)
    extra = None if extra_input is None else torch.as_tensor(np.asarray(extra_input), dtype=torch.float32)
    n_users, n_items = a_all.shape

    def batch(users):
        rows = a_all[users]
        a = torch.as_tensor(rows.toarray() if sparse.issparse(rows) else rows, dtype=torch.float32)
        return (a if extra is None else torch.cat([a, extra[users]], 1)), a, w_all[users]

    def chunks(users):
        return [users[i:i + batch_size] for i in range(0, len(users), batch_size)]

    rng = np.random.RandomState(split_seed)
    train_users = np.sort(rng.choice(n_users, size=int(train_ratio * n_users), replace=False))
    val_users = np.setdiff1d(np.arange(n_users), train_users)

    torch.manual_seed(seed)
    np.random.seed(seed)
    model = ExposureIVAE(n_items, w_all.shape[1], latent_dim, hidden_dim, n_layers,
                         extra_dim=0 if extra is None else extra.shape[1])
    optimizer = torch.optim.Adam(model.parameters(), lr=lr, weight_decay=weight_decay)
    total_steps = int(epochs * len(train_users) / batch_size)
    anneal_steps = max(int(0.2 * total_steps / beta_max), 1)
    best = {"val": np.inf, "state": None, "epoch": 0}
    step = 0
    for epoch in range(epochs):
        model.train()
        for idx in torch.randperm(len(train_users)).split(batch_size):
            x, a, w = batch(train_users[idx.numpy()])
            anneal = min(beta_max, step / anneal_steps)
            l2 = sum(torch.norm(p) for p in model.parameters())
            loss = elbo_loss(a, *model(x, w), anneal=anneal) + l2_penalty * l2
            optimizer.zero_grad()
            loss.backward()
            optimizer.step()
            step += 1
        model.eval()
        with torch.no_grad():
            val = 0.0
            for users in chunks(val_users):
                x, a, w = batch(users)
                val += float(elbo_loss(a, *model(x, w), anneal=beta_max)) * len(users) / len(val_users)
        if val < best["val"]:
            best.update(val=val, state=copy.deepcopy(model.state_dict()), epoch=epoch + 1)
        elif epoch + 1 - best["epoch"] >= patience:
            break
        if verbose and epoch % 100 == 0:
            print(f"iVAE epoch {epoch + 1}: val loss {val:.3f} (best {best['val']:.3f} @ {best['epoch']})", flush=True)
    model.load_state_dict(best["state"])
    model.eval()
    parts = []
    with torch.no_grad():
        for users in chunks(np.arange(n_users)):
            x, _, w = batch(users)
            parts.append(model.encode(x, w))
    mean, log_var = torch.cat([m for m, _ in parts]), torch.cat([v for _, v in parts])
    return mean, torch.exp(0.5 * log_var), {"best_val_loss": best["val"], "best_epoch": best["epoch"],
                                             "epochs_run": epoch + 1}
