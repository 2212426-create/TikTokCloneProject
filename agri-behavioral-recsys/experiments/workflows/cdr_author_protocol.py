"""Tái lập MF-DR-BIAS + CDR trên Coat theo đúng mã và giao thức của tác giả CDR.

Port từ github.com/CrazyDumpling/CDR_CIKM2023 (CDR/mf_cdr_bias_coat.py, CDR/MF_mcdropout.py,
CDR/utils.py) để giải thích vì sao 0.6567 của bài báo không so được với benchmark dự án.
Khác giao thức iDCF của dự án ở các điểm:
  - rating >= 3 là dương (dự án: >= 4);
  - chấm trên toàn bộ random log, không tách validation, dừng theo train loss;
  - user không có item dương được tính NDCG = Recall = 1; Recall chia cho min(k, số dương);
  - propensity Naive Bayes ước lượng từ 5% nhãn của chính tập test.
Mô hình trong benchmark (src/causal/cdr) không bị thay đổi.

    python experiments/run.py cdr-author --seeds 2020 42 43 44 45
"""

import argparse
import json
import time
from pathlib import Path

import numpy as np
import torch
import torch.nn.functional as F
from torch import nn

from src.data_pipeline.coat_dataset import load_coat_raw_features
from src.utils.helpers import RESULTS_DIR


def mc_dropout(x, p):
    # Mặt nạ không co giãn (không chia 1-p), như McDropout của tác giả.
    return (torch.rand(x.shape) > p).float() * x


class DotMF(nn.Module):
    """MF không bias, khởi tạo Xavier normal (MF_BaseModel / MF_McdropoutModel)."""

    def __init__(self, n_users, n_items, k):
        super().__init__()
        self.W = nn.Embedding(n_users, k)
        self.H = nn.Embedding(n_items, k)
        nn.init.xavier_normal_(self.W.weight)
        nn.init.xavier_normal_(self.H.weight)

    def forward(self, x):
        return (self.W(x[:, 0]) * self.H(x[:, 1])).sum(1)

    def sample_predict(self, x, n, p):
        with torch.no_grad():
            preds = torch.stack([(mc_dropout(self.W(x[:, 0]), p) * mc_dropout(self.H(x[:, 1]), p)).sum(1)
                                 for _ in range(n)])
        return mc_dropout(torch.sigmoid(preds), p)


def sample_bce(pred, imputation):
    losses = torch.stack([F.binary_cross_entropy(pred, imp, reduction="none") for imp in imputation])
    mean = losses.mean(0)
    return mean, ((losses - mean) ** 2).mean(0)


def naive_bayes_inverse_propensity(x, y, y_ips):
    py1 = y_ips.sum() / len(y_ips)
    po1 = len(x) / (x[:, 0].max() * x[:, 1].max())  # giữ nguyên cách tính của tác giả
    py1o1 = y.sum() / len(y)
    prop = np.where(y == 1, py1o1 * po1 / py1, (1 - py1o1) * po1 / (1 - py1))
    return torch.tensor(1.0 / prop, dtype=torch.float32)


def fit_cdr_bias(x, y, y_ips, n_users, n_items, k=4, un_thres=10.0, pdrop=0.5, lr=0.05, lamb=1e-3,
                 batch_size=128, G=4, tol=1e-5, stop=5, max_epochs=1000, n_samples=10,
                 inv_prop=None, on_epoch=None):
    """on_epoch(pred_model, epoch) -> True để dừng (dùng cho chọn epoch bằng validation);
    khi không có, dừng theo train loss như tác giả. inv_prop thay propensity Naive Bayes nếu cho trước."""
    pred_model, imp_model = DotMF(n_users, n_items, k), DotMF(n_users, n_items, k)
    opt_pred = torch.optim.Adam(pred_model.parameters(), lr=lr, weight_decay=lamb)
    opt_imp = torch.optim.Adam(imp_model.parameters(), lr=lr, weight_decay=lamb)
    x_all = torch.tensor(np.array([[u, i] for u in range(n_users) for i in range(n_items)]))
    x_t, y_t = torch.tensor(x), torch.tensor(y, dtype=torch.float32)
    if inv_prop is None:
        inv_prop = naive_bayes_inverse_propensity(x, y, y_ips)
    last_loss, early, total_batch = 1e9, 0, len(x) // batch_size
    for epoch in range(max_epochs):
        perm, ul = np.random.permutation(len(x)), np.random.permutation(len(x_all))
        epoch_loss = 0.0
        for b in range(total_batch):
            sel = perm[b * batch_size:(b + 1) * batch_size]
            sub_x, sub_y, w = x_t[sel], y_t[sel], inv_prop[sel]
            pred = torch.sigmoid(pred_model(sub_x))
            imp_y = imp_model.sample_predict(sub_x, n_samples, pdrop)
            x_s = x_all[ul[G * b * batch_size:G * (b + 1) * batch_size]]
            pred_u = torch.sigmoid(pred_model(x_s))
            imp_y1 = imp_model.sample_predict(x_s, n_samples, pdrop)
            xent = F.binary_cross_entropy(pred, sub_y, weight=w, reduction="sum")
            imp_loss, imp_unc = sample_bce(pred, imp_y)
            direct, direct_unc = sample_bce(pred_u, imp_y1)
            # Bộ lọc CDR của tác giả: phương sai sai số imputation < un_thres.
            loss = (xent - (imp_loss * (imp_unc < un_thres)).sum()
                    + (direct * (direct_unc < un_thres)).sum()) / len(x_s)
            opt_pred.zero_grad()
            loss.backward()
            opt_pred.step()
            epoch_loss += float(xent.detach())

            with torch.no_grad():
                pred = torch.sigmoid(pred_model(sub_x))
            imp = torch.sigmoid(imp_model(sub_x))
            e = F.binary_cross_entropy(pred, sub_y, reduction="none")
            e_hat = F.binary_cross_entropy(imp, pred, reduction="none")
            imp_obj = (((e - e_hat) ** 2) * w ** 3 * (1 - 1 / w) ** 2).sum()
            opt_imp.zero_grad()
            imp_obj.backward()
            opt_imp.step()
        if on_epoch is not None:
            if on_epoch(pred_model, epoch + 1):
                break
            continue
        if (last_loss - epoch_loss) / (last_loss + 1e-10) < tol:
            if early > stop:
                break
            early += 1
        last_loss = epoch_loss
    return pred_model, epoch + 1


def author_metrics(scores, x_te, y_te, k=5):
    """ndcg_func / recall_func của tác giả: user không có dương được tính 1."""
    ndcgs, recalls = [], []
    disc = 1 / np.log2(np.arange(2, k + 2))
    for u in np.unique(x_te[:, 0]):
        m = x_te[:, 0] == u
        y_u, s_u = y_te[m], scores[m]
        top = np.argsort(-s_u)[:k]
        ideal = np.sort(y_u)[::-1][:k]
        best = (ideal * disc[:len(ideal)]).sum()
        ndcgs.append(1.0 if best == 0 else (y_u[top] * disc[:len(top)]).sum() / best)
        # recall_func của tác giả chia cho min(k, số dương), không phải tổng số dương.
        recalls.append(1.0 if ideal.sum() == 0 else y_u[top].sum() / ideal.sum())
    return float(np.mean(ndcgs)), float(np.mean(recalls))


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--seeds", nargs="+", type=int, default=[2020, 42, 43, 44, 45])
    parser.add_argument("--threshold", type=float, default=3.0)
    args = parser.parse_args(argv)

    raw = load_coat_raw_features()
    train, test = raw["train_matrix"], raw["test_matrix"]
    n_users, n_items = train.shape
    tr_u, tr_i = np.nonzero(train)
    te_u, te_i = np.nonzero(test)
    x_tr, x_te = np.stack([tr_u, tr_i], 1), np.stack([te_u, te_i], 1)
    y_tr = (train[tr_u, tr_i] >= args.threshold).astype(np.float32)
    y_te = (test[te_u, te_i] >= args.threshold).astype(np.float32)

    rows = []
    for seed in args.seeds:
        np.random.seed(seed)
        torch.manual_seed(seed)
        ips_idx = np.random.permutation(len(y_te))
        y_ips = y_te[ips_idx[:int(0.05 * len(ips_idx))]]
        t0 = time.time()
        model, epochs = fit_cdr_bias(x_tr, y_tr, y_ips, n_users, n_items)
        with torch.no_grad():
            scores = model(torch.tensor(x_te)).numpy()
        rand = np.mean([author_metrics(np.random.rand(len(y_te)), x_te, y_te) for _ in range(20)], axis=0)
        ndcg, recall = author_metrics(scores, x_te, y_te)
        rows.append({"seed": seed, "NDCG@5": ndcg, "Recall@5": recall, "epochs": epochs,
                     "random_NDCG@5": float(rand[0]), "random_Recall@5": float(rand[1]),
                     "seconds": round(time.time() - t0, 1)})
        print(f"seed {seed}: CDR NDCG@5={ndcg:.4f} Recall@5={recall:.4f} | random NDCG@5={rand[0]:.4f} "
              f"| {epochs} epochs", flush=True)

    nd = np.array([r["NDCG@5"] for r in rows]); rc = np.array([r["Recall@5"] for r in rows])
    rn = np.array([r["random_NDCG@5"] for r in rows])
    summary = {"NDCG@5_mean": float(nd.mean()), "NDCG@5_std": float(nd.std(ddof=1)) if len(nd) > 1 else None,
               "Recall@5_mean": float(rc.mean()), "Recall@5_std": float(rc.std(ddof=1)) if len(rc) > 1 else None,
               "random_NDCG@5_mean": float(rn.mean()), "paper_NDCG@5": 0.6567, "paper_Recall@5": 0.6678,
               "threshold": args.threshold,
               "positive_rate_test": float(y_te.mean()),
               "zero_positive_test_users": int(sum(y_te[x_te[:, 0] == u].sum() == 0 for u in np.unique(x_te[:, 0])))}
    path = Path(RESULTS_DIR) / f"cdr_author_protocol_coat_thr{int(args.threshold)}.json"
    path.write_text(json.dumps({"summary": summary, "runs": rows}, indent=2), encoding="utf-8")
    print(json.dumps(summary, indent=2))
    print(f"Report: {path}")


if __name__ == "__main__":
    main()
