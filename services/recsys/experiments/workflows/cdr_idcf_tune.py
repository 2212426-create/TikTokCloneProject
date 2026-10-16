"""Tối ưu DR-BIAS + CDR trên Coat theo giao thức iDCF của dự án (rating >= 4, split user 30/70).

Thuật toán là bản port của tác giả CDR (experiments/workflows/cdr_author_protocol.py):
lọc imputation theo phương sai < un_thres, MF không bias, MC dropout không co giãn.
Khác bản tác giả ở chỗ không chạm vào test:
  - propensity Naive Bayes ước lượng từ nhãn random log của user VALIDATION
    ("nb_val"), hoặc propensity Coat có sẵn kẹp 20 ("coat_file", như benchmark);
  - chọn epoch bằng validation NDCG@5 (patience), chọn cấu hình bằng validation seed đầu;
  - cố định cấu hình rồi lặp seed, chấm test một lần mỗi seed bằng evaluator chung.

    python experiments/run.py cdr-tune
"""

import argparse
import copy
import itertools
import json
import time
from pathlib import Path

import numpy as np
import torch

from experiments.workflows.cdr_author_protocol import fit_cdr_bias, naive_bayes_inverse_propensity
from src.data_pipeline.coat_dataset import load_coat_processed, load_coat_raw_features
from src.evaluation.evaluator import evaluate_unbiased_model
from src.evaluation.protocol import DEFAULT_SEEDS, TOP_K
from src.utils.helpers import RESULTS_DIR


class Factors:
    """Bọc DotMF cho evaluator chung (điểm = <W_u, H_i>)."""

    def __init__(self, model):
        self.u = model.W.weight.detach().cpu().numpy()
        self.i = model.H.weight.detach().cpu().numpy()

    def eval(self):
        return self

    def get_evaluation_factors(self):
        return self.u, self.i


def inverse_propensity(kind, data, x, y, cap=20.0):
    if kind == "coat_file":
        prop = load_coat_raw_features(data.get("data_dir"))["propensities"][x[:, 0], x[:, 1]]
        return torch.tensor(np.minimum(1.0 / np.clip(prop, 1e-4, 1.0), cap), dtype=torch.float32)
    # Nhãn random log của user validation: không dùng nhãn test.
    y_val = np.array([float(i in data["val_ground_truth"][u]) for u, items in data["val_candidates"].items()
                      for i in items], dtype=np.float32)
    return naive_bayes_inverse_propensity(x, y, y_val)


def run(data, config, seed, max_epochs, patience, split="val"):
    np.random.seed(seed)
    torch.manual_seed(seed)
    x = np.stack([data["train_u"], data["train_i"]], 1).astype(np.int64)
    y = data["train_y"].astype(np.float32)
    w = inverse_propensity(config["propensity"], data, x, y)
    best = {"ndcg": -1.0, "epoch": 0, "state": None}
    include = data.get("evaluation_include_zero_positive_users", True)

    def on_epoch(model, epoch):
        score = evaluate_unbiased_model(Factors(model), data["val_candidates"], data["val_ground_truth"],
                                        k=TOP_K, include_zero_positive_users=include)["NDCG@5"]
        if score > best["ndcg"]:
            best.update(ndcg=score, epoch=epoch, state=copy.deepcopy(model.state_dict()))
        return epoch - best["epoch"] >= patience

    model, epochs = fit_cdr_bias(x, y, None, data["n_users"], data["n_items"], k=config["k"],
                                 lr=config["lr"], lamb=config["lamb"], un_thres=config["un_thres"],
                                 inv_prop=w, on_epoch=on_epoch, max_epochs=max_epochs)
    model.load_state_dict(best["state"])
    out = {"best_val_ndcg": best["ndcg"], "best_epoch": best["epoch"], "epochs_run": epochs}
    if split == "test":
        res = evaluate_unbiased_model(Factors(model), data["test_candidates"], data["test_ground_truth"],
                                      k=TOP_K, include_zero_positive_users=include)
        out.update({"NDCG@5": res["NDCG@5"], "Recall@5": res["Recall@5"]})
    return out


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--max-epochs", type=int, default=200)
    parser.add_argument("--patience", type=int, default=15)
    parser.add_argument("--seeds", nargs="+", type=int, default=list(DEFAULT_SEEDS))
    args = parser.parse_args(argv)

    data = load_coat_processed()
    grid = [dict(propensity=p, k=k, lr=lr, lamb=1e-3, un_thres=u)
            for p, k, lr, u in itertools.product(["nb_val", "coat_file"], [4, 32], [0.01, 0.05], [10.0, 0.1])]
    trials = []
    for config in grid:
        t0 = time.time()
        r = run(data, config, args.seeds[0], args.max_epochs, args.patience)
        trials.append({"config": config, **r, "seconds": round(time.time() - t0, 1)})
        print(f"{config}: val NDCG@5={r['best_val_ndcg']:.4f} (epoch {r['best_epoch']})", flush=True)
    selected = max(trials, key=lambda t: t["best_val_ndcg"])["config"]
    print(f"Selected on validation: {selected}", flush=True)

    rows = []
    for seed in args.seeds:
        r = run(data, selected, seed, args.max_epochs, args.patience, split="test")
        rows.append({"seed": seed, **r})
        print(f"seed {seed}: test NDCG@5={r['NDCG@5']:.4f} Recall@5={r['Recall@5']:.4f}", flush=True)
    nd = np.array([r["NDCG@5"] for r in rows]); rc = np.array([r["Recall@5"] for r in rows])
    summary = {"selected": selected, "NDCG@5_mean": float(nd.mean()), "NDCG@5_std": float(nd.std(ddof=1)),
               "Recall@5_mean": float(rc.mean()), "Recall@5_std": float(rc.std(ddof=1)), "n_seeds": len(rows)}
    path = Path(RESULTS_DIR) / "cdr_idcf_tune_coat.json"
    path.write_text(json.dumps({"summary": summary, "trials": trials, "runs": rows}, indent=2), encoding="utf-8")
    print(json.dumps(summary, indent=2))
    print(f"Report: {path}")


if __name__ == "__main__":
    main()
