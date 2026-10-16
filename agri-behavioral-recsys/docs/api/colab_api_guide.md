# RecSys API cho app TopTop

Mã nguồn: [`src/api/`](../../src/api/). Kiểm thử: [`tests/test_api.py`](../../tests/test_api.py).

## Mô hình phục vụ

| `model` | Cách tính điểm | Huấn luyện |
| :-- | :-- | :-- |
| `mf` | sigmoid(⟨p_u, q_i⟩ + b_u + b_i + μ) | Adam + BCE trên tương tác TopTop đã ghi log, L2 trên embedding |
| `cdr` | Mô hình gợi ý của DR-BIAS + CDR | Propensity = tần suất exposure theo item (kẹp 0,05–1), cặp đối chứng lấy mẫu đều |
| `popularity` | log(1 + watchCount + 5·totalLikes) + log(1 + số lượt dương) | Không học; dùng cho user chưa có lịch sử |
| `ividr` | Không phục vụ | Cần đặc trưng user làm IV/proxy; API trả về `mf` kèm `fallback_reason` |

Nhãn dương: `is_like` hoặc `play_time_ms ≥ 3000`. Cặp user–video lặp được gộp, dương nếu có ít nhất một lần dương.
Mô hình được huấn luyện lại sau mỗi `retrain_every` (mặc định 20) tương tác mới, hoặc khi gọi `/api/v1/sync`.

## Nguồn dữ liệu

`RECSYS_DATA_SOURCE` = `auto` (mặc định) | `live` | `export`:

- Video và user: Firestore REST. Nếu Firestore không trả video nào, dùng `firestore_data.json`
  hoặc `toptop_processed_bundle.json` trong `RECSYS_EXPORT_DIR`.
- Tương tác: `toptop_interactions.csv` (export), lượt like trong RTDB `Notifications`
  (chỉ giữ khi `fromUsername` ánh xạ duy nhất tới một uid) và log của API
  (`RECSYS_INTERACTION_LOG`, mặc định `data/api/interactions_log.csv`).

Firestore rules chỉ cho khách chưa đăng nhập đọc video `approved`. Tại 2026-10-15 truy vấn công khai
không trả video nào, và bản export có 84/85 video `rejected`. App gửi `filter_rejected=true`, nên
feed AI chỉ có nội dung khi có video được duyệt.

## Endpoint

| Method | Path | Ghi chú |
| :-- | :-- | :-- |
| GET | `/health` | Nguồn dữ liệu, số tương tác, mô hình đã huấn luyện |
| POST | `/api/v1/recommend` | `{user_id, k, model, filter_rejected, exclude_video_ids}` → `recommendations` (khóa camelCase của Firestore), `model_used`, `fallback_reason` |
| POST | `/api/v1/interaction` | `{user_id, video_id, play_time_ms, is_like}` |
| POST | `/api/v1/compare` | Top-K của mọi mô hình khả dụng cho cùng user |
| GET | `/api/v1/items/popular` | Top-K theo độ phổ biến |
| POST | `/api/v1/sync` | Nạp lại dữ liệu và huấn luyện lại |

## Chạy

Cục bộ:

```powershell
.\venv\Scripts\python.exe -m uvicorn src.api.app:app --port 8000
```

Có Cloudflare Tunnel và ghi URL vào `server_config/recsys_url` để app tự kết nối:

```powershell
.\venv\Scripts\python.exe -m src.api.tunnel --port 8000
```

Google Colab: mở `notebooks/TopTop_RecSys_Colab_API.ipynb` (sinh bằng `notebooks/build_notebook.py`),
chọn Run all. Notebook clone repo từ GitHub, nên cần push `src/api` trước.
