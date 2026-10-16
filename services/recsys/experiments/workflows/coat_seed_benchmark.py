"""Coat: MF, iDCF, IViDR (theo giai đoạn) và DR-BIAS + CDR, lặp 10 seed (42–51).

Mỗi seed chọn checkpoint bằng validation rồi chấm test một lần. Báo mean ± sample std
theo seed, Wilcoxon theo cặp seed giữa các mô hình, và đặt cạnh số bài báo.

    python experiments/run.py coat-seeds [--models MF iDCF IViDR "DR-BIAS + CDR"]
"""

import argparse
import json
import tempfile
import time
from pathlib import Path

import numpy as np
from scipy.stats import wilcoxon

from src.baselines.train_mf import train_baseline_mf
from src.causal.cdr.train_dr_bias_cdr import train_dr_bias_cdr
from src.causal.idcf.ividr_staged import mf_item_embeddings
from src.causal.idcf.train_idcf import fit_idcf_pipeline
from src.data_pipeline.coat_dataset import load_coat_processed
from src.evaluation.protocol import DEFAULT_SEEDS
from src.utils.artifacts import data_signature, digest, source_signature
from src.utils.helpers import RESULTS_DIR

# Bảng tham chiếu NDCG@5 / Recall@5 trên Coat (docs/benchmarks/paper_reference_comparison.md).
# iDCF lấy theo ablation trong bài IViDR (bảng chính iDCF ghi 0.5744).
PAPER = {"MF": (0.5524, 0.5294), "iDCF": (0.5638, 0.5493), "IViDR": (0.5903, 0.5783),
         "DR-BIAS + CDR": (0.6567, 0.6678)}
# Cấu hình MF của tác giả iDCF (MF.py); cũng là treatment T của IViDR.
MF_CONFIG = dict(embedding_dim=64, epochs=100, lr=5e-4, weight_decay=1e-6, loss_name="mse", decay_scope="all",
                 scheduler_name="none", train_global_bias=False, patience=5, batch_size=512)


def run_seed(name, data, seed, ckpt):
    if name in ("MF", "IViDR"):
        mf = train_baseline_mf(dataset_name="coat", data_dict=data, seed=seed, checkpoint_dir=ckpt,
                               evaluate_test=name == "MF", checkpoint_tag=f"seeds_mf_{seed}", **MF_CONFIG)
        if name == "MF":
            return mf
        return fit_idcf_pipeline(data, "coat", seed, item_emb=mf_item_embeddings(mf["checkpoint_path"]),
                                 checkpoint_dir=ckpt, checkpoint_tag=f"seeds_ividr_{seed}")
    if name == "iDCF":
        return fit_idcf_pipeline(data, "coat", seed, checkpoint_dir=ckpt, checkpoint_tag=f"seeds_idcf_{seed}")
    # Warm start MF-MSE + chế độ tác giả: validation tốt nhất trong ablation 3 seed
    # (0.607 so với 0.540 của warm BCE + lọc CV 0.01); xem docs/benchmarks/root_cause_analysis.md.
    warm = train_baseline_mf(dataset_name="coat", data_dict=data, seed=seed, checkpoint_dir=ckpt,
                             evaluate_test=False, checkpoint_tag=f"seeds_cdr_warm_{seed}",
                             **{**MF_CONFIG, "embedding_dim": 32})
    return train_dr_bias_cdr(dataset_name="coat", data_dict=data, seed=seed, embedding_dim=32,
                             un_thres=10.0, filter_mode="variance", ips_weighting="author",
                             counterfactual_pool="all", max_inverse_propensity=20.0, epochs=100, lr=0.002,
                             batch_size=128, initial_rec_checkpoint=warm["checkpoint_path"],
                             checkpoint_tag=f"seeds_cdr_{seed}")


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--seeds", nargs="+", type=int, default=list(DEFAULT_SEEDS))
    parser.add_argument("--models", nargs="+", choices=list(PAPER), default=list(PAPER))
    args = parser.parse_args(argv)

    data = load_coat_processed()
    identity = {"dataset": "coat", "seeds": args.seeds, "models": args.models,
                "data": data_signature(data), "code_sha256": source_signature()}
    path = Path(RESULTS_DIR) / f"coat_seeds_{digest(identity)}.json"
    rows = {name: [] for name in args.models}
    with tempfile.TemporaryDirectory() as ckpt:
        for name in args.models:
            for seed in args.seeds:
                t0 = time.time()
                r = run_seed(name, data, seed, ckpt)
                rows[name].append({"seed": seed, "NDCG@5": r["NDCG@5"], "Recall@5": r["Recall@5"],
                                   "best_epoch": r["best_epoch"], "best_val_ndcg": r["best_val_ndcg"],
                                   "seconds": round(time.time() - t0, 1)})
                print(f"{name} seed {seed}: NDCG@5={r['NDCG@5']:.4f} Recall@5={r['Recall@5']:.4f}", flush=True)

    summary = {}
    for name, runs in rows.items():
        nd = np.array([x["NDCG@5"] for x in runs]); rc = np.array([x["Recall@5"] for x in runs])
        summary[name] = {"NDCG@5_mean": float(nd.mean()), "NDCG@5_std": float(nd.std(ddof=1)) if len(nd) > 1 else None,
                         "Recall@5_mean": float(rc.mean()), "Recall@5_std": float(rc.std(ddof=1)) if len(rc) > 1 else None,
                         "paper_NDCG@5": PAPER[name][0], "paper_Recall@5": PAPER[name][1]}
    pairs = {}
    for a in args.models:
        for b in args.models:
            if a < b:
                diff = np.array([x["NDCG@5"] for x in rows[a]]) - np.array([x["NDCG@5"] for x in rows[b]])
                p = float(wilcoxon(diff).pvalue) if np.any(diff) else 1.0
                pairs[f"{a} - {b}"] = {"mean_delta_ndcg": float(diff.mean()), "wilcoxon_p_over_seeds": p}
    report = {"identity": identity, "runs": rows, "summary": summary, "seed_pairs": pairs}
    path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    lines = [f"# Coat — {len(args.seeds)} seed ({args.seeds[0]}–{args.seeds[-1]})", "",
             "| Mô hình | NDCG@5 (local) | Recall@5 (local) | NDCG@5 bài báo | Recall@5 bài báo |",
             "| :-- | --: | --: | --: | --: |"]
    for name, s in summary.items():
        lines.append(f"| {name} | {s['NDCG@5_mean']:.4f} ± {s['NDCG@5_std'] or 0:.4f} | "
                     f"{s['Recall@5_mean']:.4f} ± {s['Recall@5_std'] or 0:.4f} | {s['paper_NDCG@5']:.4f} | {s['paper_Recall@5']:.4f} |")
    lines += ["", "| So sánh | ΔNDCG@5 trung bình | Wilcoxon p (theo cặp seed) |", "| :-- | --: | --: |"]
    lines += [f"| {k} | {v['mean_delta_ndcg']:+.4f} | {v['wilcoxon_p_over_seeds']:.3g} |" for k, v in pairs.items()]
    lines += ["", "CDR bài báo dùng giao thức khác (rating ≥ 3, user không dương tính 1); không so trực tiếp được.",
              "Xem docs/benchmarks/root_cause_analysis.md.", ""]
    path.with_suffix(".md").write_text("\n".join(lines), encoding="utf-8")
    print("\n".join(lines))
    print(f"Report: {path}")


if __name__ == "__main__":
    main()
