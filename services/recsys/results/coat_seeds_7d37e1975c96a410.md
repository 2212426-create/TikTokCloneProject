# Coat — 10 seed (42–51), IViDR loss = mse

| Mô hình | NDCG@5 (local) | Recall@5 (local) | NDCG@5 bài báo | Recall@5 bài báo |
| :-- | --: | --: | --: | --: |
| iDCF | 0.5704 ± 0.0124 | 0.5516 ± 0.0124 | 0.5638 | 0.5493 |
| IViDR-staged | 0.5712 ± 0.0129 | 0.5549 ± 0.0157 | 0.5903 | 0.5783 |

| So sánh | ΔNDCG@5 trung bình | Wilcoxon p (theo cặp seed) |
| :-- | --: | --: |
| IViDR-staged - iDCF | +0.0009 | 0.275 |

CDR bài báo dùng giao thức khác (rating ≥ 3, user không dương tính 1); không so trực tiếp được.
Xem docs/benchmarks/root_cause_analysis.md.
