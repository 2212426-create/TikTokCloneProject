# Benchmark v4: validation và test độc lập

**Benchmark đầy đủ dữ liệu; một seed huấn luyện, chọn checkpoint bằng validation.**

**Thời gian cập nhật:** 2026-10-16 03:50:20

Chỉ xếp hạng các exposure đã ghi log. Chọn epoch trên validation; test giữ riêng.
Chỉ số chính: NDCG@5 và Recall@5. AUC là chỉ số phụ lấy trung bình theo user có hai lớp; không mặc định tương đương AUC trong bài CDR.
Coat và KuaiRand chia random log theo user, 30% validation / 70% test; macro metric chỉ chấm user có nhãn dương.
Kết quả địa phương chưa thể so trực tiếp với bảng bài báo: mô hình, siêu tham số, seed và giao thức giữa các bài báo còn khác. Bảng tham chiếu có chú giải riêng tại `docs/paper_reference_comparison.md`.

### Kết quả trên held-out test:

| Tập Dữ Liệu       | Mô Hình Thuật Toán | NDCG@5 | Recall@5 | Precision@5 | HitRate@5 | AUC (macro-user) |
| :---------------- | :----------------- | :----: | :------: | :---------: | :-------: | :--------------: |
| **KuaiRand-Pure** | MF                 | 0.3744 |  0.3246  |   0.2873    |  0.7493   |      0.5897      |
| **KuaiRand-Pure** | DR-BIAS + CDR      | 0.3459 |  0.2990  |   0.2694    |  0.7102   |      0.5609      |
| **KuaiRand-Pure** | IViDR              | 0.3630 |  0.3188  |   0.2779    |  0.7362   |      0.5885      |

### Candidate pool kuairand

- Users: 16,473; zero-positive users: 2,971; candidates: 671,360.
- Positive prevalence: 0.1764; candidate size p10/p50/p90: [10.0, 20.0, 93.0].
- Cold users: 0.00%; cold candidates: 0.00%.
- Validation positive prevalence: 0.1782; validation candidate p50: 20.

- MF: ΔNDCG@5 so với Popularity = +0.0166; CI95 NDCG của mô hình, theo user ≈ ±0.0051.
- IViDR: ΔNDCG@5 so với Popularity = +0.0052; CI95 NDCG của mô hình, theo user ≈ ±0.0050.
- DR-BIAS + CDR: ΔNDCG@5 so với Popularity = -0.0119; CI95 NDCG của mô hình, theo user ≈ ±0.0050.

### Kiểm định theo cặp user (kuairand, test)

| So sánh | ΔNDCG@5 | CI95 | Wilcoxon p | Thắng / thua (user) |
| :-- | --: | --: | --: | :--: |
| MF vs Popularity | +0.0166 | ±0.0041 | 4.7e-21 | 5423 / 4472 |
| IViDR vs Popularity | +0.0052 | ±0.0051 | 0.0209 | 5491 / 5233 |
| IViDR vs MF | -0.0114 | ±0.0039 | 1.1e-09 | 4643 / 5175 |
| DR-BIAS + CDR vs Popularity | -0.0119 | ±0.0049 | 0.000708 | 5078 / 5310 |
| DR-BIAS + CDR vs MF | -0.0285 | ±0.0031 | 1.17e-72 | 3300 / 4849 |

p < 0.05: khác biệt có ý nghĩa thống kê trên tập user này (một seed huấn luyện; chưa tính biến thiên seed).

### Giới hạn sai số

- Khoảng tin cậy trong JSON là xấp xỉ theo người dùng của một lần chạy; chưa bao gồm biến thiên do seed hoặc chọn cấu hình.
- Random (expected) là kỳ vọng xếp hạng ngẫu nhiên có điều kiện trên candidate pool hiện có; Popularity dùng tần suất exposure trong train.
- KuaiRand dùng duy nhất is_click, ghép hai standard log, lọc user/item tối thiểu 10 exposure một lượt và chia random log theo user. CDR dùng tần suất exposure theo item làm proxy propensity.
- Coat dùng rating ≥ 4 là dương; 6.960 biased ratings để train và 4.640 random ratings chia theo user seed 1234. Các cặp trùng train/test giữ nguyên để đối chiếu giao thức bài báo.
- IViDR huấn luyện decoder tái tạo exposure từ biased train: toàn bộ item ở Coat và 128 item lấy mẫu đều mỗi batch ở KuaiRand.
