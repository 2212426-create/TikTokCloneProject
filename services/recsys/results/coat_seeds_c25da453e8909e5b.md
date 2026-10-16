# Coat — 10 seed (42–51), IViDR loss = mse

| Mô hình | NDCG@5 (local) | Recall@5 (local) | NDCG@5 bài báo | Recall@5 bài báo |
| :-- | --: | --: | --: | --: |
| MF | 0.5402 ± 0.0118 | 0.5240 ± 0.0179 | 0.5524 | 0.5294 |
| IViDR | 0.5440 ± 0.0147 | 0.5463 ± 0.0193 | 0.5903 | 0.5783 |
| DR-BIAS + CDR | 0.4756 ± 0.0064 | 0.4937 ± 0.0095 | 0.6567 | 0.6678 |

| So sánh | ΔNDCG@5 trung bình | Wilcoxon p (theo cặp seed) |
| :-- | --: | --: |
| IViDR - MF | +0.0038 | 0.492 |
| DR-BIAS + CDR - MF | -0.0646 | 0.00195 |
| DR-BIAS + CDR - IViDR | -0.0684 | 0.00195 |

CDR bài báo dùng split 10/90 và biến thể tốt nhất theo dataset; không so trực tiếp được.
