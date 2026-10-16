# 📚 Trung Tâm Tài Liệu Nghiên Cứu & Kỹ Thuật (Documentation Hub)

Thư mục này quản lý tập trung toàn bộ tài liệu nghiên cứu lý thuyết, đề cương đồ án, bài báo khoa học gốc, hướng dẫn kỹ thuật triển khai API, tài liệu thuyết trình và báo cáo thực nghiệm của hệ thống gợi ý **TopTop RecSys** (`services/recsys`).

---

## 🗂️ Cấu Trúc Thư Mục Chuẩn Hóa

```text
services/recsys/docs/
├── README.md                           # Bản chỉ mục trung tâm này
│
├── 📑 de_cuong/                         # Đề cương đồ án chuyên ngành & phân chia nhiệm vụ
│   ├── DeCuong_NghienCuuUngDungHocMayDeXuatVideoNgan.md  # Đề cương tổng hợp (CF + Deep Learning + API)
│   ├── DeCuong_Phan1_LocCongTac_CF.md                     # Đề cương chi tiết Phần 1: Lọc cộng tác (CF/MF/CDR)
│   └── DeCuong_Phan2_HocSau_DeepLearning.md               # Đề cương chi tiết Phần 2: Học sâu (NCF/iVAE/IViDR)
│
├── 🎤 presentation/                     # Tài liệu báo cáo & thuyết trình bảo vệ đồ án
│   ├── presentation_slides.md          # Nội dung trình chiếu Slide Marp chuẩn hóa (15 slides)
│   └── presentation_script.md          # Kịch bản thuyết trình chi tiết (lời thoại, căn thời gian, Q&A)
│
├── 🚀 api/                              # Triển khai RESTful API & Google Colab Service
│   ├── colab_api_guide.md              # Cẩm nang vận hành RecSys API (src/api), Colab & Cloudflare Tunnel
│   └── colab_recsys_rest_api_plan.md   # Bản thiết kế cũ (lịch sử), đã được thay bằng src/api
│
├── 🔬 benchmarks/                       # Giao thức đánh giá khoa học & Nhật ký thực nghiệm
│   ├── evaluation_protocol.md          # Giao thức đánh giá chuẩn (NDCG@5, Recall@5, Held-Out Split)
│   ├── mf_optimizer_audit.md           # Kiểm toán thuật toán MF & so sánh thực nghiệm Adam vs SGD
│   ├── paper_reference_comparison.md   # Đối chiếu kết quả với các công bố gốc (iDCF, IViDR, CDR)
│   ├── benchmark_v2_plan_and_error_analysis.md # Phân tích lỗi và kế hoạch benchmark v2
│   ├── kuairand_tuning_2026-09-29.md   # Nhật ký tinh chỉnh siêu tham số trên KuaiRand
│   ├── table3_run.md                   # Lịch sử: Table 3 với IViDR joint (mã đã gỡ)
│   └── root_cause_analysis.md          # Vì sao MF, CDR, IViDR thấp hơn bài báo & kế hoạch sửa
│
├── 📝 paper_summaries/                 # Tóm tắt lý thuyết & phân tích chi tiết bài báo khoa học
│   ├── CDR.md                          # CIKM 2023 - Conservative Doubly Robust
│   ├── iDCF.md                         # KDD 2023 - Identifiable Debiasing in Collaborative Filtering
│   ├── IViDR.md                        # KDD 2023 - Instrumental Variable & Identifiable VAE
│   └── ReCRec.md                       # WSDM 2023 - Representation Counterfactual Debiasing
│
└── 📄 papers/                           # Tệp PDF bài báo khoa học gốc
    ├── CDR.pdf                         # Bản in gốc bài báo CDR (CIKM 2023)
    ├── iDCF.pdf                        # Bản in gốc bài báo iDCF (KDD 2023)
    ├── IViDR.pdf                       # Bản in gốc bài báo IViDR (KDD 2023)
    └── ReCRec.pdf                      # Bản in gốc bài báo ReCRec (WSDM 2023)
```

---

## 📖 Hướng Dẫn Truy Cập Nhanh

### 1. Đề Cương & Quản Lý Đồ Án Chuyên Ngành
* **Đề cương tổng hợp:** [`de_cuong/DeCuong_NghienCuuUngDungHocMayDeXuatVideoNgan.md`](de_cuong/DeCuong_NghienCuuUngDungHocMayDeXuatVideoNgan.md) — Phân tách 2 trụ cột (CF & Deep Learning), 12 mốc tiến độ.
* **Phần 1 (Lọc cộng tác):** [`de_cuong/DeCuong_Phan1_LocCongTac_CF.md`](de_cuong/DeCuong_Phan1_LocCongTac_CF.md) — Sinh viên Vũ Thế Huỳnh phụ trách.
* **Phần 2 (Học sâu):** [`de_cuong/DeCuong_Phan2_HocSau_DeepLearning.md`](de_cuong/DeCuong_Phan2_HocSau_DeepLearning.md) — Sinh viên Phùng Nguyễn Hoài Bo phụ trách.

### 2. Thuyết Trình & Bảo Vệ Đồ Án
* **Slide báo cáo:** [`presentation/presentation_slides.md`](presentation/presentation_slides.md) — Nội dung 15 slide chuẩn Marp/PDF.
* **Kịch bản thuyết minh:** [`presentation/presentation_script.md`](presentation/presentation_script.md) — Lời thoại chi tiết từng câu, phân bổ thời lượng 7-8.5 phút, cẩm nang 8 câu hỏi Q&A phản biện.

### 3. Vận Hành Kỹ Thuật & Tích Hợp API
* **Hướng dẫn chạy Colab RecSys Server:** [`api/colab_api_guide.md`](api/colab_api_guide.md) — Khởi động FastAPI, Cloudflare Tunnel, Zero-Config URL discovery.
* **Kiến trúc & Đặc tả API:** [`api/colab_recsys_rest_api_plan.md`](api/colab_recsys_rest_api_plan.md) — Chi tiết schema `/api/v1/recommend` và `/api/v1/interaction`.

### 4. Nghiên Cứu Thực Nghiệm & Đo Lường
* **Giao thức đo lường:** [`benchmarks/evaluation_protocol.md`](benchmarks/evaluation_protocol.md) — Quy chuẩn chia dữ liệu (30% Val / 70% Test với Seed 1234), chỉ số Macro-User NDCG@5, Recall@5, AUC.
* **Phân tích Adam vs SGD:** [`benchmarks/mf_optimizer_audit.md`](benchmarks/mf_optimizer_audit.md) — Lý giải bản chất gradient thưa trong RecSys.
* **Nguyên nhân chênh lệch với bài báo:** [`benchmarks/root_cause_analysis.md`](benchmarks/root_cause_analysis.md) — Loss MSE vs BCE, giao thức CDR, kiến trúc iDCF/IViDR.
* **Đối chiếu số liệu paper:** [`benchmarks/paper_reference_comparison.md`](benchmarks/paper_reference_comparison.md) — Phân tích ranh giới khoa học giữa benchmark dự án và các bài báo gốc.
* **Tóm tắt bài báo:** Tham khảo thư mục [`paper_summaries/`](paper_summaries/) kèm đối chiếu PDF trong [`papers/`](papers/).
