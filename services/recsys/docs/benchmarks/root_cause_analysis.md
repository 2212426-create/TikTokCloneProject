# Vì sao MF, DR-BIAS + CDR và IViDR thấp hơn bài báo

Cập nhật: 2026-10-16 (review lần 2: lỗi R1 ở nhánh IV, gỡ IViDR joint). Đối chiếu trực tiếp với mã tác giả
[iDCF](https://github.com/BgmLover/iDCF) (nền của MF và IViDR trong bảng Table 3 IViDR) và
[CDR](https://github.com/CrazyDumpling/CDR_CIKM2023). Mọi số là NDCG@5 trên tập test,
chọn checkpoint bằng validation, giao thức iDCF của dự án, trừ khi ghi khác.

## Tóm tắt

### Coat (10 seed 42–51, mean ± std; Wilcoxon theo cặp seed so với MF)

| Mô hình | Trước | Hiện tại | Bài báo | Δ vs MF (p) | Nguyên nhân chính | Trạng thái |
| :-- | --: | --: | --: | :-- | :-- | :-- |
| MF | 0.465 | **0.540 ± 0.012** | 0.5524 | — | Loss BCE thay vì MSE (L-MF-1) | Đã sửa, khớp bài báo |
| iDCF (2 giai đoạn, mới) | — | **0.570 ± 0.012** | 0.5638 / 0.5744† | +0.030 (p = 0.002, thắng 10/10) | — | Tái lập được |
| IViDR theo giai đoạn (mới) | — | **0.571 ± 0.013** | 0.5903 | +0.031 (p = 0.002, thắng 10/10) | Kiến trúc joint (L-IV-1); nhánh IV vô hiệu (R1, L-IV-2) | Đã sửa kiến trúc và R1; chờ chạy lại |
| IViDR joint (cũ) | 0.531 (BCE) | 0.544 ± 0.015 | 0.5903 | +0.004 (p = 0.49) | L-IV-1 | Thay bằng bản theo giai đoạn |
| DR-BIAS + CDR | 0.476 ± 0.006 | 0.509 ± 0.030 | 0.6567* | −0.032 (p = 0.004) | Giao thức bài báo khác (L-CDR-1); CDR là khung BCE (L-CDR-4) | Tái lập 0.666 theo giao thức tác giả |

†0.5638 là iDCF trong ablation của bài IViDR; 0.5744 là iDCF trong bảng chính.
\*Giao thức khác, không so trực tiếp (L-CDR-1).

### KuaiRand-Pure (seed 42, cấu hình mới; `results/benchmark_kuairand_all_2edb44f179_v4.md`)

| Mô hình | Trước | Hiện tại | Bài báo |
| :-- | --: | --: | --: |
| MF | 0.343 | **0.3744** | 0.3748 |
| IViDR joint | — | 0.3630 | 0.4161 |
| DR-BIAS + CDR | — | 0.3459 | 0.3098‡ |
| Popularity | 0.3578 | 0.3578 | — |

‡Giao thức CDR khác. iDCF / IViDR theo giai đoạn chưa chạy trên KuaiRand.

**Tiền xử lý dữ liệu không phải nguyên nhân.** Nhãn (Coat rating ≥ 4, KuaiRand `is_click`),
lọc user rồi item ≥ 10 exposure một lượt, ánh xạ ID, chia user random log 30/70 bằng
`RandomState(1234)`, chỉ xếp hạng item trong random log, NDCG/Recall loại user không có
dương: khớp `data_process/*/build_dataset.ipynb` và `utils.py` của iDCF. Bằng chứng: MF và
iDCF đúng cấu hình tác giả cho số trùng hoặc cao hơn bài báo.

## MF

**L-MF-1 (chính) — Loss BCE thay vì MSE.** `iDCF/MF.py` dùng `nn.MSELoss()` trên điểm thô
(không sigmoid) với nhãn 0/1; dự án mặc định `BCEWithLogits`. Ablation Coat, 10 seed 42–51:

| Cấu hình | NDCG@5 |
| :-- | --: |
| A. Benchmark cũ: BCE, lr 1e-3, wd 1e-5, decay chỉ embedding, ReduceLROnPlateau, μ học được, patience 7 | 0.465 ± 0.014 |
| B. Đúng tác giả: MSE, lr 5e-4, wd 1e-6, decay mọi tham số, không scheduler, μ = 0, patience 5 | **0.540 ± 0.012** |
| C. = B nhưng BCE | 0.453 ± 0.012 |
| D. = A nhưng lr/wd của tác giả | 0.455 ± 0.013 |

B và C chỉ khác loss: chênh **+0.087**. A và D cho thấy lr/wd gần như không tác động.
KuaiRand (lr 5e-5, dim 64, batch 2048, cùng seed): MSE 0.3744, BCE 0.3316.

**L-MF-2 — Quá khớp sớm với BCE.** BCE đạt đỉnh validation ở epoch 4–7 rồi giảm; MSE ở
epoch 11–22 (Coat), 22 (KuaiRand). Gradient MSE trên điểm thô bị chặn, đóng vai trò điều
chuẩn ngầm mạnh hơn trên dữ liệu MNAR nhỏ (giả thuyết, chưa kiểm chứng riêng).

**L-MF-3 — Siêu tham số KuaiRand.** Benchmark cũ dùng dim 128, lr 5e-4, batch 4096, trần
25 epoch; tác giả dùng dim 64, lr 5e-5, batch 2048, 100 epoch. Đây là lý do mọi mô hình
từng thua Popularity (0.358) trên KuaiRand.

**L-MF-4 (phụ).** Khởi tạo Normal thay vì Uniform(±0.01), intercept học được thay vì cố
định 0, decay chỉ embedding, scheduler plateau. Đã gỡ cùng cấu hình B.

Đã sửa: `experiments/workflows/run_all_benchmarks.py` dùng cấu hình B; MF KuaiRand 100 epoch.

## DR-BIAS + CDR

**L-CDR-1 (chính) — Giao thức chấm của bài báo khác và dễ hơn.** Theo `CDR/utils.py` và
`CDR/mf_cdr_bias_coat.py`:

- `binarize(thres=3)`: rating ≥ **3** là dương. Tỷ lệ dương trong random log 40% (dự án 18,5%).
- `ndcg_func`/`recall_func`: user không có dương được tính **1**; Recall chia `min(k, #dương)`.
- Chấm toàn bộ random log, không có validation; dừng theo train loss.
- Propensity Naive Bayes ước lượng từ **5% nhãn của chính tập test**.

Xếp hạng **ngẫu nhiên** đạt NDCG@5 ≈ **0.46** theo giao thức này (dự án ≈ 0.30). Bản port
thuật toán tác giả của dự án (`experiments/run.py cdr-author`) đạt **0.666 ± 0.006**,
Recall 0.672 ± 0.007 (5 seed) so với bài báo 0.6567 / 0.6678: thuật toán đúng, chênh lệch là
do thang đo. Số 0.6567 không đặt cạnh MF 0.5524 của iDCF được.

**L-CDR-2 — Bộ lọc imputation: giả thuyết lỗi bị bác bỏ.** `src/causal/cdr` lọc
`std/mean < 0.01` nên bỏ ~99% imputation, còn tác giả lọc `phương sai < 10` (giữ gần hết).
Ablation Coat 3 seed (warm start MF-BCE trừ dòng cuối; chọn bằng validation):

| Biến thể | Val NDCG@5 | Test NDCG@5 | Epoch tốt nhất |
| :-- | --: | --: | :-- |
| Cũ: lọc CV 0.01, IPS `(1/p − 1)`, counterfactual chỉ cặp chưa quan sát | 0.540 | 0.479 | 14–36 |
| Chỉ đổi bộ lọc sang phương sai < 10 | 0.489 | 0.445 | 6–11 |
| Đủ chế độ tác giả (lọc phương sai, IPS không trọng số, toàn U×I) | 0.528 | 0.470 | 16–18 |
| Chế độ tác giả + warm start MF-MSE | **0.607** | **0.524** | **1** |

Giữ lại gần hết imputation làm **giảm** điểm: bộ lọc gắt đang giúp CDR, không phải lỗi.
Dòng cuối có validation tốt nhất nên được chọn làm mặc định Coat
(`filter_mode="variance"`, `ips_weighting="author"`, `counterfactual_pool="all"`,
`un_thres=10`, warm start MF-MSE dim 32), cho **0.509 ± 0.030** trên 10 seed. Nhưng epoch
tốt nhất luôn là 1: điểm đến từ warm start MF-MSE, huấn luyện CDR (BCE) từ epoch 2 trở đi
chỉ làm validation giảm.

**L-CDR-3 — Khác công thức so với mã tác giả.** Các khác biệt dưới đây đã thêm thành tùy
chọn trong `src/causal/cdr/` (`filter_mode`, `ips_weighting`, `counterfactual_pool`); như
L-CDR-2 cho thấy, chúng không giải thích khoảng cách.

| Thành phần | Dự án (mặc định cũ) | Tác giả |
| :-- | :-- | :-- |
| Phần IPS | trừ `(1/p − 1)·ê·filter` | trừ `ê·filter`, không trọng số |
| Counterfactual | chỉ cặp chưa quan sát | toàn bộ U×I |
| MC dropout | co giãn 1/(1−p), không dropout đầu ra | mặt nạ không co giãn, dropout cả xác suất |
| Backbone | MF có bias, warm-start MF-BCE, dim 32, lr 0.002 | MF không bias Xavier, dim 4, lr 0.05 |
| Propensity | file Coat, kẹp 20 | Naive Bayes từ nhãn test |

**L-CDR-4 (chính dưới giao thức dự án) — CDR là khung BCE.** Sai số e trong doubly-robust là
BCE nên CDR chịu cùng điểm yếu L-MF-1. Bằng chứng độc lập: bản port **đúng mã tác giả**,
tune theo giao thức dự án (`experiments/run.py cdr-tune`: 16 cấu hình, chọn bằng validation,
propensity Naive Bayes từ nhãn validation thay vì test), cấu hình chọn (dim 32, lr 0.01,
un_thres 10) đạt test **0.482 ± 0.008** (10 seed) — ngang CDR của `src/` và thấp hơn MF-MSE.
Kết luận: dưới giao thức iDCF, CDR thấp không do lỗi cài đặt.

## IViDR

**L-IV-0 (đã sửa) — Loss BCE.** `iDCF.py` dùng MSE. Bản joint đổi sang MSE: 0.531 ± 0.018 →
0.549 ± 0.003 (5 seed); 10 seed 0.544 ± 0.015, không hơn MF có ý nghĩa (p = 0.49).

**L-IV-1 (chính, đã sửa) — Kiến trúc joint thay vì 2 giai đoạn của iDCF.** IViDR = iDCF + IV
(ablation bài báo: iDCF 0.5638 → IViDR-F 0.5846 → IViDR 0.5903). Mã iDCF:

1. `ivae_exposure.py`: huấn luyện iVAE **riêng** (KL anneal, chọn theo val loss) trên
   **vector exposure A ∈ {0,1}^n_items** của mỗi user, W = đặc trưng user one-hot.
2. `iDCF.py::MFMF`: điểm = MF + ⟨item_emb_i, C_u⟩ với C cố định, MSE, lr 5e-4, wd 1e-6.

Bản cũ của dự án (`src/causal/ividr/`) huấn luyện **chung** loss gợi ý + 0.01·ELBO, đầu vào
iVAE là **embedding MF của user** chứ không phải A, thêm LayerNorm và φ = 2.

Đã port đúng 2 giai đoạn vào `src/causal/idcf/` (`ivae.py`, `idcf_model.py`, `train_idcf.py`):
- Với `mean.pt/std.pt` tác giả công bố: ≈ 0.586 (kiểm chứng port giai đoạn 2).
- iVAE tự huấn luyện, 10 seed: **0.570 ± 0.012**, vượt MF ở cả 10 seed (p = 0.002), cao hơn
  0.5638 của bài báo.

IViDR theo giai đoạn (`src/causal/idcf/ividr_staged.py`, Algorithm 1): T = embedding item
MF-MSE; 2SLS theo item với IV là đặc trưng trung bình của user đã thấy item; C2 = iVAE trên A,
C1 = iVAE trên [A, X^re_u]; C = 0.9·C1 + 0.9·C2; giai đoạn 2 như iDCF. Lượt 10 seed đầu tiên:
**0.571 ± 0.013** (+0.031 vs MF, p = 0.002, thắng 10/10) — nhưng chạy với lỗi R1 bên dưới.

**L-IV-2 (còn lại) — Nhánh IV là giả định; lượt đầu có lỗi (R1).** IViDR-staged chỉ hơn iDCF
**+0.0009** (Wilcoxon p = 0.28). Review lần 2 tìm ra nguyên nhân trực tiếp: `iv_reconstruct`
trả `T^re = T̂ + alpha_residual·T̃` với mặc định `alpha_residual = 1`, nên `T^re = T` — bước
2SLS **không có tác dụng**, C1 chỉ thấy embedding MF gốc. Bài báo (Eq. 8) dùng `α1·T̂ + α2·T̃`
với α học bằng MLP; IViDR-F (chỉ phần fitted) đạt 0.5846. Đã đổi mặc định sang
`alpha_residual = 0` (IViDR-F) và chọn lại bằng validation; số 10 seed cần chạy lại. Ngoài
ra Z, T, X^re và cách ghép C1/C2 vẫn là giả định vì tác giả IViDR không công bố mã.

**L-IV-3 — Tuning.** Bài báo grid lr {1e-3, 5e-4, 1e-4, 5e-5, 1e-5} × wd {1e-5, 1e-6};
dự án dùng cấu hình iDCF cố định (lr 5e-4, wd 1e-6, latent 16), chưa tune.

**L-IV-4 — KuaiRand.** Bản joint (proxy one-hot 155 chiều, IV theo `tab`) đạt 0.3630, vẫn
thua MF (0.3744): cùng nguyên nhân L-IV-1. Bản joint đã gỡ; `benchmark` nay chạy iDCF và
IViDR theo giai đoạn trên KuaiRand (exposure = mọi cặp trong standard log, W one-hot,
Z theo `tab`; lr 1e-4, batch 2048 theo mã tác giả). Chưa có số.

## Còn lại

1. Ablation `alpha_residual` ∈ {0, 0.5, 1} trên validation Coat, rồi chạy lại IViDR 10 seed.
2. Chạy `benchmark --dataset kuairand --model idcf` và `--model ividr`.
3. Thử thêm cách dựng nhánh IV (Z, ρ/τ, α học được) để thu hẹp khoảng cách với bài báo IViDR.

## Lệnh tái lập

```powershell
.\venv\Scripts\python.exe experiments\run.py coat-seeds      # Coat 10 seed: MF, iDCF, IViDR, CDR
.\venv\Scripts\python.exe experiments\run.py benchmark       # Coat + KuaiRand, 1 seed: MF, iDCF, IViDR, CDR
.\venv\Scripts\python.exe experiments\run.py cdr-author      # CDR, giao thức tác giả
.\venv\Scripts\python.exe experiments\run.py cdr-tune        # CDR port tác giả, giao thức dự án
```

Artifact: `results/coat_seeds_c25da453e8909e5b` (MF, IViDR joint, CDR cũ),
`results/coat_seeds_7d37e1975c96a410` (iDCF, IViDR-staged với lỗi R1),
`results/coat_seeds_7ae4942b7c4a9f32` (CDR mặc định mới), `results/cdr_idcf_tune_coat.json`,
`results/cdr_author_protocol_coat_thr3.json`, `results/benchmark_kuairand_all_2edb44f179_v4`.
