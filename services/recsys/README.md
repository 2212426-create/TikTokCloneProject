# Benchmark MF, iDCF, IViDR và DR-BIAS + CDR

Benchmark dùng cấu hình MF của mã tác giả iDCF: **Adam + MSE trên điểm thô** của exposure
đã quan sát (phân rã ma trận có bias, không phải ALS). iDCF và IViDR huấn luyện 2 giai đoạn
như mã tác giả iDCF (`src/causal/idcf/`). Vì sao kết quả từng thấp hơn bài báo và đã sửa gì:
[root_cause_analysis.md](docs/benchmarks/root_cause_analysis.md).

## Một đầu chạy cho test và thí nghiệm

Tất cả lệnh đi qua `experiments/run.py`; xem tùy chọn bằng `COMMAND --help`.
Ba file test cũ đã gom thành `tests/test_suite.py`. Các triển khai thí nghiệm nằm
trong `experiments/workflows/`, không chạy trực tiếp các file bên trong.

| Lệnh | Mục đích |
| :-- | :-- |
| `test` | Toàn bộ kiểm thử dữ liệu, metric, gradient, checkpoint và CLI; cần raw Coat |
| `smoke` | Chạy nhanh mọi mô hình trên Coat và mẫu 64 user KuaiRand |
| `benchmark [--dataset coat/kuairand] [--model mf/idcf/ividr/cdr]` | Train/đánh giá trên dữ liệu đầy đủ, một seed; hỗ trợ grid MF KuaiRand |
| `coat-seeds [--models ...]` | Coat, 10 seed: MF, iDCF, IViDR, DR-BIAS + CDR; Wilcoxon theo seed |
| `cdr-author` / `cdr-tune` | CDR port mã tác giả: theo giao thức tác giả / theo giao thức dự án |
| `mf` | Một lượt MF với optimizer/loss tùy chọn |
| `tune` | Tuning Coat; chọn cấu hình bằng validation |
| `tune-kr --model mf` | Ví dụ tuning KuaiRand-Pure trên validation; chỉ chấm test cho cấu hình được chọn |
| `optimizers --dataset coat hoặc kuairand` | So sánh Adam/SGD, tune bằng validation rồi lặp seed |
| `evaluate --dataset coat hoặc kuairand --checkpoint FILE` | Chấm checkpoint của dữ liệu đầy đủ, không train lại |
| `protocol` | In toàn bộ quy tắc metric đang dùng |
| `clean` / `clean --apply` | Xem trước / xóa checkpoint, `__pycache__` và log sinh ra |

```powershell
.\venv\Scripts\python.exe experiments\run.py test
.\venv\Scripts\python.exe experiments\run.py smoke
.\venv\Scripts\python.exe experiments\run.py protocol
```

Thống nhất NDCG@5/Recall@5 theo công thức và quy tắc user của iDCF; các lượt lặp
seed mặc định **10 seed, 42–51**, báo mean ± sample std. Đây là dãy seed địa phương,
không khẳng định trùng dãy của tác giả. `benchmark` một seed và `smoke` không phải
kết quả nhiều seed. Xem [giao thức và khác biệt giữa các bài](docs/benchmarks/evaluation_protocol.md).

iDCF/IViDR dùng proxy W = đặc trưng user one-hot (Coat `user_features.ascii`; KuaiRand
theo notebook tác giả) và exposure A của user. IViDR thêm biến công cụ Z (Coat: đặc trưng
user; KuaiRand: tỷ lệ exposure theo `tab`) để tái tạo treatment bằng 2SLS — phần này là
giả định địa phương vì tác giả IViDR chưa công bố mã. Báo cáo có kiểm định Wilcoxon theo
cặp user cho từng mô hình so với Popularity và MF.

## RecSys API cho app TopTop

`src/api/` phục vụ MF và DR-BIAS + CDR huấn luyện trên tương tác TopTop, cùng
popularity cho user mới. Xem [hướng dẫn API](docs/api/colab_api_guide.md).

```powershell
.\venv\Scripts\python.exe -m uvicorn src.api.app:app --port 8000
```

## Môi trường và dữ liệu

Chạy lệnh từ thư mục `services/recsys`. Đã kiểm tra Python 3.12.10,
PyTorch 2.14.1+cpu; máy hiện tại không có CUDA khả dụng.

```powershell
py -3.12 -m venv venv
.\venv\Scripts\python.exe -m pip install -r requirements.txt
.\venv\Scripts\python.exe experiments\run.py test
```

File bắt buộc:

```text
data/raw/
  coat/
    train.ascii
    test.ascii
    propensities.ascii
    user_item_features/user_features.ascii
    user_item_features/item_features.ascii
  kuairand_pure/data/
    log_standard_4_08_to_4_21_pure.csv
    log_standard_4_22_to_5_08_pure.csv
    log_random_4_22_to_5_08_pure.csv
    user_features_pure.csv
```

Loader ưu tiên `./data/` nếu tồn tại, nếu không dùng `../data/`. API `data_dir` chỉ đến
dataset cụ thể, không fallback. KuaiRand cũng hỗ trợ file trực tiếp trong
`kuairand_pure/`. User features cần cho IViDR; MF chỉ cần log.

## Giao thức

| Thành phần | Coat | KuaiRand-Pure |
| :-- | :-- | :-- |
| Positive | rating >= 4 | is_click = 1 |
| Train | 6.960 biased ratings | Hai standard log; lọc user rồi item >=10 exposure một lượt |
| Kích thước | 290 user, 300 item | 23.533 user, 6.712 video; 1.413.574 train exposure |
| Random log | 4.640 ratings | 954.814 exposure sau lọc |
| Validation/test | Chia user 30/70, seed 1234 | Cùng quy tắc |

Giữ cả positive và negative đã quan sát; không tự gán unknown pairs thành negative.
KuaiRand gộp cặp lặp trong candidate pool, positive nếu có ít nhất một click.
NDCG@5/Recall@5 macro chỉ tính user có positive, báo số user bị loại. Recall chia
cho tổng positive; `Recall@5_bounded` là metric riêng. AUC chỉ tính user có hai lớp.
Coat giữ 366 cặp train/random trùng, trong đó 247 thuộc test; cần công bố khi so sánh.
Đây là ranking trên logged candidates, không phải full-catalog ranking.

## Chạy MF và so sánh bộ tối ưu

```powershell
# Baseline mặc định, chọn checkpoint bằng validation
.\venv\Scripts\python.exe experiments\run.py benchmark --dataset coat --model mf --epochs 25

# Chọn optimizer/loss rõ ràng qua trainer MF
.\venv\Scripts\python.exe experiments\run.py mf --dataset coat --optimizer sgd --loss bce --lr 0.1 --epochs 50

# MSE + Adam, regularize mọi tham số, không scheduler.
# Đây vẫn là kiến trúc địa phương, KHÔNG phải tái lập nguyên bản iDCF.
.\venv\Scripts\python.exe experiments\run.py mf --dataset coat --optimizer adam --loss mse --decay-scope all --scheduler none --lr 0.0005 --weight-decay 0.000001 --dim 64 --epochs 100

# Adam/SGD: mỗi optimizer 3 learning rate x 2 weight decay, rồi cố định config và lặp seed
.\venv\Scripts\python.exe experiments\run.py optimizers --dataset coat --epochs 50
.\venv\Scripts\python.exe experiments\run.py optimizers --dataset kuairand --epochs 50
```

`experiments/run.py optimizers` dùng cùng loss, dim, batch, initialization, split và
giới hạn epoch/patience. Learning rate mặc định Adam: .001/.005/.01;
SGD thuần (momentum=0): .01/.1/1. Weight decay: 1e-5/1e-4.
Mỗi optimizer chọn cấu hình trên validation seed đầu tiên, đóng băng cấu hình rồi
chấm checkpoint thắng và lặp seed. Test không chọn cấu hình hay optimizer.
Report JSON ghi mean/std, thời gian trial, seed, source/data hash và checkpoint.
Ngân sách epoch bằng nhau không bảo đảm hội tụ hay thời gian bằng nhau.
Thêm `--loss mse --decay-scope all` để so sánh với mục tiêu MSE; không trộn
hai loss rồi diễn giải chênh lệch thành tác động riêng của optimizer.

Runner `experiments/run.py benchmark`, tuning cũ và causal trainer vẫn mặc định Adam.
`--quick` giảm epoch; `--smoke-users` mới giảm dữ liệu KuaiRand và chỉ dùng kèm
`--quick --dataset kuairand`. Smoke không chứng minh chất lượng hay hội tụ.

Benchmark mặc định dành tối đa 100 epoch cho Coat và 25 epoch cho KuaiRand;
IViDR trên Coat dùng patience 20 vì validation có thể tiếp tục cải thiện sau
epoch 25. Có thể đặt riêng bằng `--epochs-coat` và `--epochs-kr`; `--epochs`
ghi đè cả hai. Cấu hình KuaiRand được ghi trong báo cáo tuning tương ứng.

```powershell
.\venv\Scripts\python.exe experiments\run.py benchmark --dataset coat --quick --epochs 1
.\venv\Scripts\python.exe experiments\run.py benchmark --dataset kuairand --quick --smoke-users 64 --dim 16 --batch-size 256 --epochs 1
.\venv\Scripts\python.exe experiments\run.py tune --model all --epochs 50
```

## Dọn dữ liệu sinh ra

```powershell
# Xem trước đường dẫn
.\venv\Scripts\python.exe experiments\run.py clean
# Xóa cache, checkpoint, report/log chạy và bytecode; giữ results/README.md
.\venv\Scripts\python.exe experiments\run.py clean --apply
```

Script giữ raw datasets, venv, source và papers; từ chối đường dẫn vượt dự án
hoặc qua junction/symlink. Chỉ dọn khi không có training đang chạy. Cache sẽ
được dựng lại. Ngày 2026-09-27 đã dọn toàn bộ artifact chạy cũ; các báo cáo
mới là kiểm chứng sau sửa. Xem [kết quả hiện tại](results/README.md).

Checkpoint v4 ghi optimizer, loss, decay scope, scheduler, config, data/source
hash. `evaluate_checkpoint` nạp checkpoint đã chọn, không train lại. Kiến trúc
MF không đổi; tên file mới phân biệt thí nghiệm bằng metadata hash.
Checkpoint thiếu `metric_revision=logged_binary_v1` bị từ chối khi chấm bằng
giao thức mới; cần train lại. Không gộp report cũ vào bảng kết quả hiện tại.
