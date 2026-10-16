# Trạng thái kết quả

Mỗi báo cáo `benchmark_*_v4.json/.md` ghi kèm hash cấu hình; checkpoint tương ứng
nằm trong `checkpoints/`. Báo cáo mới có thêm bảng kiểm định Wilcoxon theo cặp user.

Xem [kiểm tra siêu tham số KuaiRand-Pure](../docs/benchmarks/kuairand_tuning_2026-09-29.md)
và [phân tích nguyên nhân chênh lệch với bài báo](../docs/benchmarks/root_cause_analysis.md).
`coat_seeds_*.json/.md` là các lượt Coat 10 seed (`run.py coat-seeds`).

Từ thư mục `agri-behavioral-recsys`, chạy:

```powershell
.\venv\Scripts\python.exe experiments\run.py benchmark --dataset kuairand
```

Lệnh `experiments\run.py clean` xem trước các file sinh ra;
`experiments\run.py clean --apply` xóa chúng sau khi các tiến trình train dừng.
