# Số tham chiếu bài báo và điều kiện so sánh

> Phân tích nguyên nhân chênh lệch giữa kết quả dự án và các số dưới đây: [root_cause_analysis.md](root_cause_analysis.md).

Bảng tham chiếu được giữ từ bảng người dùng cung cấp và tài liệu nghiên cứu của dự án. Đây là **số tham chiếu bên ngoài**, không phải kết quả chạy v4. Yahoo hiện chưa có pipeline trong dự án.

| Mô hình | Coat NDCG@5 | Coat Recall@5 | Yahoo NDCG@5 | Yahoo Recall@5 | Kuai NDCG@5 | Kuai Recall@5 |
| :-- | --: | --: | --: | --: | --: | --: |
| MF | 0.5524 | 0.5294 | 0.5629 | 0.7129 | 0.3748 | 0.3247 |
| DeepDCF-MF | 0.5373 | 0.5141 | 0.6395 | 0.7729 | 0.4078 | 0.3491 |
| iDCF | 0.5744 | 0.5504 | 0.6455 | 0.7837 | 0.4093 | 0.3513 |
| ReCRec | 0.5946 | 0.5787 | 0.6872 | 0.8124 | 0.3979 | 0.3285 |
| CDR, biến thể tốt nhất theo dataset | 0.6567 | 0.6678 | 0.6565 | 0.7323 | 0.3167 | 0.3078 |
| IViDR | 0.5903 | 0.5783 | 0.6602 | 0.7901 | 0.4161 | 0.3549 |

Dòng CDR ghép biến thể tốt nhất theo dataset: DR-BIAS+CDR cho Coat/Yahoo; MRDR+CDR cho KuaiRand. Không dùng dòng ghép này làm kết quả của một mô hình DR-BIAS+CDR duy nhất. Tài liệu dự án ghi riêng DR-BIAS+CDR KuaiRand là 0.3098 / 0.3048; cần giữ đúng nhãn biến thể khi đối chiếu.

## Nguồn

- [iDCF, Debiasing Recommendation by Learning Identifiable Latent Confounders (2023)](https://arxiv.org/pdf/2302.05052): nguồn tham khảo nhóm baseline MF, DeepDCF-MF và iDCF. [Notebook KuaiRand](https://github.com/BgmLover/iDCF/blob/main/data_process/kuairand/build_dataset.ipynb) và [mã split/evaluation](https://github.com/BgmLover/iDCF/blob/main/utils.py) là căn cứ cho preprocessing/split hiện tại của dự án.
- [CDR, CIKM 2023](https://jiawei-chen.github.io/paper/CIKM23-CDR.pdf), [mã nguồn tác giả](https://github.com/CrazyDumpling/CDR_CIKM2023): lưu ý biến thể DR-BIAS/MRDR, tỷ lệ validation/test và dữ liệu sau lọc khác giao thức iDCF.
- [IViDR, arXiv 2024](https://arxiv.org/pdf/2410.12451): tài liệu cơ chế IV và iVAE. Bản joint cũ đã gỡ; IViDR hiện huấn luyện theo giai đoạn trên nền iDCF, nhánh IV vẫn là giả định địa phương. Xem [root_cause_analysis.md](root_cause_analysis.md).
- [ReCRec (2024)](https://zhoushengisnoob.github.io/papers/TOIS2024.pdf): kiểm tra đúng biến thể, split và ngân sách tuning trước khi đặt cạnh điểm địa phương.

## Điều kiện cho bảng so sánh trực tiếp

Phải thống nhất phiên bản dữ liệu, nhãn, quy tắc lọc/gộp exposure, user split, overlap train/test, candidate pool, user không positive, công thức Recall, backbone và ngân sách tuning. Sau đó cố định cấu hình bằng validation, chạy nhiều seed và báo mean ± std. Hiện chưa đủ điều kiện kết luận thắng/thua giữa các dòng trên và benchmark dự án.

[Kết quả địa phương có artifact](../results/README.md) · [Phân tích sai số](benchmark_v2_plan_and_error_analysis.md)

## Nguyên nhân chênh lệch với bài báo (đo ngày 2026-10-15)

Đối chiếu mã tác giả iDCF (`MF.py`, `models/mf.py`, `utils.py`, `argparser.py`,
`data_process/*/build_dataset.ipynb`):

- **Khớp:** chỉ xếp hạng item trong random log (`predict_matrix.data += 1 << 10`),
  công thức NDCG/Recall, loại user không có dương, split user 30/70 seed 1234,
  nhãn Coat rating ≥ 4, nhãn KuaiRand `is_click` (threshold 0,9 trên 0/1), lọc ≥ 10.
- **Khác và là nguyên nhân chính:** tác giả huấn luyện MF bằng **MSE trên điểm thô**
  với nhãn 0/1; dự án dùng BCEWithLogits.

### Ablation MF trên Coat (10 seed 42–51, test NDCG@5)

| Cấu hình | NDCG@5 | Recall@5 | Epoch tốt nhất |
| :-- | --: | --: | :-- |
| A. Benchmark cũ: BCE, lr 1e-3, wd 1e-5, decay embedding, plateau | 0,4646 ± 0,0141 | 0,4804 ± 0,0184 | 4–7 (một seed 35) |
| B. Mã tác giả: MSE, lr 5e-4, wd 1e-6, decay mọi tham số, không scheduler, μ = 0 | **0,5402 ± 0,0118** | 0,5240 ± 0,0179 | 11–22 |
| C. B nhưng BCE | 0,4533 ± 0,0121 | 0,4681 ± 0,0185 | 2–12 |
| D. A nhưng lr/wd tác giả | 0,4547 ± 0,0130 | 0,4668 ± 0,0210 | 2–15 |

Bài báo: 0,5524 / 0,5294. Đổi BCE → MSE (+0,08) giải thích gần hết khoảng cách;
lr/wd gần như không ảnh hưởng. BCE đạt đỉnh rất sớm rồi quá khớp; MSE với điểm thô
cho gradient bị chặn và hội tụ chậm hơn — đây là giả thuyết, chưa kiểm chứng riêng.

### MF trên KuaiRand-Pure (seed 42, test)

| Cấu hình | NDCG@5 | Recall@5 | Epoch tốt nhất |
| :-- | --: | --: | --: |
| Benchmark cũ (BCE, lr 5e-4, dim 128, trần 25 epoch) | 0,3434 | 0,2948 | 6 |
| Mã tác giả (MSE, lr 5e-5, wd 1e-6, dim 64, batch 2048, ≤ 100 epoch) | **0,3744** | 0,3246 | 22 |
| Mã tác giả nhưng BCE | 0,3316 | 0,2845 | 27 |

Bài báo: 0,3748 / 0,3247. Popularity trên cùng test: 0,3578; trước đây mọi mô hình thua
Popularity là do loss BCE, không phải do dữ liệu.

### IViDR trên Coat (5 seed 42–46, IV tách khỏi proxy)

| Loss gợi ý | lr | NDCG@5 | Recall@5 |
| :-- | :-- | --: | --: |
| BCE | 3e-3 | 0,5312 ± 0,0176 | 0,5353 ± 0,0149 |
| **MSE** | 3e-3 | **0,5494 ± 0,0034** | 0,5530 ± 0,0117 |
| MSE | 5e-4 | 0,5343 ± 0,0550 | 0,5381 ± 0,0536 |

Bài báo: 0,5903 / 0,5783. Benchmark nay dùng MSE cho MF và IViDR (chọn để khớp mã tác giả;
các số test ở đây là chẩn đoán, không dùng để chọn). Khoảng cách IViDR còn lại (~0,04) nhiều
khả năng do bản IViDR địa phương không có mã tác giả (huấn luyện chung thay vì 3 giai đoạn,
cách tạo X/Z/W, LayerNorm, clipping) — xem [table3_run.md](table3_run.md).

### Thay đổi đi kèm

- KuaiRand iDCF/IViDR: proxy W = đặc trưng user one-hot đầy đủ như
  notebook tác giả (155 chiều; NaN thành nhóm riêng), biến công cụ Z = tỷ lệ exposure theo `tab`.
- MF KuaiRand mặc định ≤ 100 epoch, patience 5 (lr 5e-5 hội tụ chậm).
- CDR giữ BCE vì sai số e trong công thức doubly-robust là BCE; điểm CDR bài báo dùng split
  10/90 nên vẫn không so trực tiếp.
- `python experiments/run.py coat-seeds` chạy 10 seed cho cả ba mô hình và ghi bảng so với bài báo.

### Kết quả 10 seed sau khi sửa MF (Coat, seed 42–51; IViDR ở đây là bản joint, mã đã gỡ)

| Mô hình | NDCG@5 | Recall@5 | NDCG@5 bài báo | Recall@5 bài báo |
| :-- | --: | --: | --: | --: |
| MF (MSE, cấu hình tác giả) | 0,5402 ± 0,0118 | 0,5240 ± 0,0179 | 0,5524 | 0,5294 |
| IViDR (MSE, IV tách proxy) | 0,5440 ± 0,0147 | 0,5463 ± 0,0193 | 0,5903 | 0,5783 |
| DR-BIAS + CDR (BCE) | 0,4756 ± 0,0064 | 0,4937 ± 0,0095 | 0,6567* | 0,6678* |

| So sánh (theo cặp seed) | ΔNDCG@5 | Wilcoxon p |
| :-- | --: | --: |
| IViDR − MF | +0,0038 | 0,49 |
| CDR − MF | −0,0646 | 0,002 |
| CDR − IViDR | −0,0684 | 0,002 |

\*Split 10/90, không so trực tiếp. Artifact: `results/coat_seeds_c25da453e8909e5b.{json,md}`.

**Diễn giải.** MF tái lập sát bài báo (−0,012, khoảng 1 std). Với baseline MF đúng cấu hình,
IViDR địa phương **không còn vượt MF có ý nghĩa** (bài báo: +0,038). Kết luận "IViDR > MF,
p = 0,002" trong benchmark một seed trước đây đến từ baseline MF yếu (BCE), không phải
từ hiệu quả khử thiên lệch. CDR địa phương (BCE, warm-start MF-BCE) kém MF-MSE rõ rệt.
Đây là hiện tượng đã được ghi nhận trong nghiên cứu tái lập RecSys: baseline được tune
đúng thường xóa phần lớn lợi thế của mô hình mới (Dacrema và cộng sự, RecSys 2019).

### Cập nhật 2026-10-16: iDCF 2 giai đoạn và IViDR theo giai đoạn (Coat, 10 seed 42–51)

| Mô hình | NDCG@5 | Recall@5 | NDCG@5 bài báo | Δ vs MF (Wilcoxon p) |
| :-- | --: | --: | --: | :-- |
| MF (MSE) | 0,5402 ± 0,0118 | 0,5240 ± 0,0179 | 0,5524 | — |
| iDCF (port 2 giai đoạn) | 0,5704 ± 0,0124 | 0,5516 ± 0,0124 | 0,5638 / 0,5744 | +0,030 (0,002) |
| IViDR theo giai đoạn | 0,5712 ± 0,0129 | 0,5549 ± 0,0157 | 0,5903 | +0,031 (0,002) |
| DR-BIAS + CDR (chế độ tác giả, warm MF-MSE) | 0,5087 ± 0,0297 | 0,5141 ± 0,0274 | 0,6567* | −0,032 (0,004) |

Kiến trúc 2 giai đoạn của iDCF khôi phục lợi thế khử thiên lệch so với MF; bản IViDR joint cũ
không có lợi thế này. Nhánh IV địa phương chưa thêm được gì so với iDCF (+0,0009, p = 0,28).
Chi tiết và ablation CDR: [root_cause_analysis.md](root_cause_analysis.md). Artifact:
`results/coat_seeds_7d37e1975c96a410.*`, `results/coat_seeds_7ae4942b7c4a9f32.*`.

### KuaiRand-Pure sau khi sửa (seed 42, `benchmark --dataset kuairand`)

| Mô hình | NDCG@5 | Recall@5 | Trước khi sửa | NDCG@5 bài báo |
| :-- | --: | --: | --: | --: |
| MF (MSE, cấu hình tác giả) | **0,3744** | 0,3246 | 0,3434 | 0,3748 |
| IViDR (MSE, Z = tab, W = one-hot đầy đủ) | 0,3630 | 0,3191 | 0,3476 | 0,4161 |
| DR-BIAS + CDR | 0,3459 | 0,2990 | 0,3459 | 0,3098* |
| Popularity | 0,3578 | | | |

Kiểm định theo cặp user (13.502 user test): MF > Popularity (+0,017, p = 5e-21);
IViDR > Popularity (+0,005, p = 0,02); **IViDR < MF** (−0,011, p = 2e-9);
CDR < MF (−0,029, p = 1e-72). Artifact: `results/benchmark_kuairand_all_32e6ded9ad_v4.md`.

MF tái lập gần đúng bài báo. IViDR tăng 0,015 so với trước nhưng vẫn kém MF và còn cách
bài báo 0,053 — phù hợp với kết luận trên Coat: phần chênh còn lại nằm ở bản cài đặt IViDR
địa phương, không ở dữ liệu hay giao thức. Chỉ một seed trên KuaiRand (mỗi lượt IViDR mất
nhiều giờ CPU).
