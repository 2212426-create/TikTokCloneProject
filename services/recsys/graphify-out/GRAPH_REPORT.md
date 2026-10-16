# Graph Report - recsys  (2026-10-16)

## Corpus Check
- 92 files · ~111,886 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 3 file(s) not represented in the graph (top: (none) 1, .pptx 1, .ipynb 1)

## Summary
- 810 nodes · 1781 edges · 53 communities (32 shown, 21 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 56 edges (avg confidence: 0.88)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `1fee46e4`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- cdr_idcf_tune.py
- firebase_bridge.py
- EngineTests
- test_api.py
- MatrixFactorization
- InteractionLog
- RecSys API cho app TopTop
- Giao thức đánh giá chung
- FusedAdamTests
- test_suite.py
- CDRFramework
- app.py
- test_methodology.py
- torch
- evaluate_unbiased_model
- CDR: Conservative Doubly Robust Learning for Debiased Recommendation
- format_grid_table
- PopularityModel
- docs/README.md
- ========================================================
- Benchmark v4: validation và test độc lập
- Giảm thiên lệch trong hệ thống gợi ý hành vi (Causal Debiasing in RecSys)
- 🎙️ KỊCH BẢN CHI TIẾT TỪNG SLIDE (SLIDE-BY-SLIDE SCRIPT)
- RecEngine
- ĐỀ CƯƠNG CHI TIẾT — PHẦN 2: HỌC SÂU TRONG HỆ THỐNG GỢI Ý (DEEP LEARNING RECOMMENDATION)
- 📡 3. Các Endpoints API Được Đóng Gói Trong Notebook Colab
- ĐỀ CƯƠNG CHI TIẾT — PHẦN 1: THUẬT TOÁN LỌC CỘNG TÁC (COLLABORATIVE FILTERING - CF)
- tunnel.py
- GPUVectorizedEvaluator
- Giao thức benchmark v4 và phân tích sai số
- presentation_slides.md
- Nguyên nhân chênh lệch với bài báo (đo ngày 2026-10-15)
- Vì sao MF, DR-BIAS + CDR và IViDR thấp hơn bài báo
- TeeLogger
- 📖 Hướng Dẫn Truy Cập Nhanh
- TeeLogger
- Benchmark MF, IViDR và DR-BIAS + CDR
- LoggedClickDataset
- Benchmark v4: validation và test độc lập
- Benchmark v4: validation và test độc lập
- Benchmark v4: validation và test độc lập
- Benchmark v4: validation và test độc lập
- Kiểm tra MF và bộ tối ưu — 2026-09-27
- coat_seeds_7ae4942b7c4a9f32.md
- coat_seeds_7d37e1975c96a410.md
- coat_seeds_c25da453e8909e5b.md

## God Nodes (most connected - your core abstractions)
1. `train_baseline_mf()` - 40 edges
2. `evaluate_checkpoint()` - 34 edges
3. `train_dr_bias_cdr()` - 30 edges
4. `evaluate_unbiased_model()` - 29 edges
5. `run_all_benchmarks()` - 28 edges
6. `load_kuairand_processed()` - 26 edges
7. `MatrixFactorization` - 26 edges
8. `load_coat_processed()` - 24 edges
9. `RecEngine` - 20 edges
10. `CDRFramework` - 20 edges

## Surprising Connections (you probably didn't know these)
- `IViDR` --references--> `iv_reconstruct()`  [INFERRED]
  docs/benchmarks/root_cause_analysis.md → src/causal/idcf/ividr_staged.py
- `Dọn dữ liệu sinh ra` --references--> `evaluate_checkpoint()`  [INFERRED]
  README.md → src/utils/artifacts.py
- `BridgeTests` --uses--> `InteractionLog`  [INFERRED]
  tests/test_api.py → src/api/firebase_bridge.py
- `RegressionTests` --uses--> `CDRFramework`  [INFERRED]
  tests/test_suite.py → src/causal/cdr/cdr_model.py
- `OptimizerTests` --uses--> `MatrixFactorization`  [INFERRED]
  tests/test_suite.py → src/models/matrix_factorization.py

## Import Cycles
- None detected.

## Communities (53 total, 21 thin omitted)

### Community 0 - "cdr_idcf_tune.py"
Cohesion: 0.10
Nodes (13): author_metrics(), DotMF, fit_cdr_bias(), main(), mc_dropout(), naive_bayes_inverse_propensity(), sample_bce(), Factors (+5 more)

### Community 1 - "firebase_bridge.py"
Cohesion: 0.17
Nodes (10): behavior_label(), fetch_firestore_collection(), fetch_rtdb(), _http_json(), _load_export_videos(), load_snapshot(), normalize_video(), read_interactions_csv() (+2 more)

### Community 2 - "EngineTests"
Cohesion: 0.11
Nodes (4): EngineTests, HttpTests, InteractionTests, make_engine()

### Community 3 - "test_api.py"
Cohesion: 0.19
Nodes (4): likes_from_notifications(), parse_fs_value(), unique_username_map(), BridgeTests

### Community 6 - "RecSys API cho app TopTop"
Cohesion: 0.40
Nodes (5): Chạy, Endpoint, Mô hình phục vụ, Nguồn dữ liệu, RecSys API cho app TopTop

### Community 9 - "Giao thức đánh giá chung"
Cohesion: 0.50
Nodes (4): Giao thức đánh giá chung, Kiểm thử, Phạm vi tương đồng với paper, Quy tắc được cố định

### Community 18 - "test_suite.py"
Cohesion: 0.06
Nodes (52): main(), main(), run_seed(), compare(), main(), generate_console_grid_report(), generate_markdown_report(), get_benchmark_tables_data() (+44 more)

### Community 24 - "app.py"
Cohesion: 0.18
Nodes (12): create_app(), compare(), get_engine(), health(), interaction(), popular(), recommend(), sync() (+4 more)

### Community 31 - "test_methodology.py"
Cohesion: 0.06
Nodes (17): root(), kuairand_inputs(), load_kuairand_author_proxy(), load_kuairand_tab_instruments(), paired_ndcg_difference(), experiment_lock(), author_user_features(), AuthorConfigTests (+9 more)

### Community 57 - "torch"
Cohesion: 0.06
Nodes (16): cdr_recommendation_loss(), dr_bias_closed_form_imputation_loss(), IDCFModel, binary_exposure(), elbo_loss(), ExposureIVAE, mlp(), train_exposure_ivae() (+8 more)

### Community 58 - "evaluate_unbiased_model"
Cohesion: 0.10
Nodes (8): evaluate_unbiased_model(), compute_auc(), compute_hitrate_at_k(), compute_ndcg_at_k(), compute_precision_at_k(), compute_recall_at_k(), FixedScores, PaperMetricTests

### Community 157 - "CDR: Conservative Doubly Robust Learning for Debiased Recommendation"
Cohesion: 0.07
Nodes (26): 1. INTRODUCTION, 2.1 Task Formulation, 2.2 Existing Estimators, 2.3 Limitation of DR, 2. ANALYSES OVER DOUBLY ROBUST LEARNING, 3.1 Conservative Doubly Robust Learning, 3.2 Theoretical Analyses, 3. METHODOLOGY (+18 more)

### Community 166 - "format_grid_table"
Cohesion: 0.10
Nodes (9): EpochTableTracker, format_grid_table(), format_markdown_table(), format_metrics_table(), get_display_width(), pad_text(), strip_formatting(), run_grid_search() (+1 more)

### Community 199 - "========================================================"
Cohesion: 0.06
Nodes (31): ========================================================, ========================================================, ========================================================, 1.1. Mục tiêu của Phần 1, 1.2. Cơ sở lý thuyết Lọc cộng tác (Chương I - Phần 1), 1.3. Triển khai mô hình & Thực nghiệm Phần 1 (Chương II - Phần 1), 1. Bối cảnh & Tính cấp thiết, 1. Tài liệu về Lọc cộng tác (Collaborative Filtering): (+23 more)

### Community 217 - "Benchmark v4: validation và test độc lập"
Cohesion: 0.33
Nodes (5): Benchmark v4: validation và test độc lập, Candidate pool kuairand, Giới hạn sai số, Kiểm định theo cặp user (kuairand, test), Kết quả trên held-out test:

### Community 222 - "Giảm thiên lệch trong hệ thống gợi ý hành vi (Causal Debiasing in RecSys)"
Cohesion: 0.11
Nodes (18): 10. Ví Dụ Định Tính Thực Tế: Ứng Dụng Video Ngắn TopTop, 11. Kết Quả Thực Nghiệm & Siêu Tham Số Tối Ưu trên COAT, 12. Kết Quả Thực Nghiệm & Siêu Tham Số Tối Ưu trên KUAIRAND, 13. Ranh Giới Khoa Học & Tính Tái Lập (Reproducibility), 14. Kết Luận & Định Hướng Phát Triển, 1. Mục Tiêu Đề Tài & Bối Cảnh Nghiên Cứu Gợi Ý Video Nông Sản, 2. Từ Thuật Toán Lọc Cộng Tác (Collaborative Filtering) Đến Sơ Đồ Nhân Quả, 3. Ba Mô Hình Lọc Cộng Tác (Collaborative Filtering) Đối Sánh (+10 more)

### Community 240 - "🎙️ KỊCH BẢN CHI TIẾT TỪNG SLIDE (SLIDE-BY-SLIDE SCRIPT)"
Cohesion: 0.07
Nodes (28): ⏱️ BẢNG ĐIỀU PHỐI THỜI GIAN NHANH (QUICK TIME CHEAT SHEET), ❓ Câu 1: Biến công cụ $Z$ trong mô hình IViDR lấy từ đâu và thỏa mãn các giả định nào?, ❓ Câu 2: Tại sao kết quả NDCG@5 của CDR trong bài báo gốc là ~0.65 mà nhóm chạy ra 0.48?, ❓ Câu 3: Cơ chế MC Dropout của CDR lọc bỏ tới 98% mẫu thì mô hình còn học được gì?, ❓ Câu 4: Vì sao trên tập KuaiRand 1.4 triệu tương tác, khoảng cách giữa các mô hình chỉ chênh lệch khoảng 0.5%?, ❓ Câu 5: Hệ thống IViDR này khi tích hợp vào App TopTop thì độ trễ như thế nào, có bị lag không?, ❓ Câu 6: Phân biệt Selection Bias và Confounding Bias trong hệ thống gợi ý?, ❓ Câu 7: Macro AUC khác gì Micro AUC và ROC-AUC truyền thống? (+20 more)

### Community 275 - "ĐỀ CƯƠNG CHI TIẾT — PHẦN 2: HỌC SÂU TRONG HỆ THỐNG GỢI Ý (DEEP LEARNING RECOMMENDATION)"
Cohesion: 0.15
Nodes (12): 🎯 1. Mục tiêu & Phạm vi nghiên cứu của Phần 2, 2.1. Neural Collaborative Filtering (NCF / NeuMF), 2.2. Mạng nơ-ron Biến phân có thể định danh (Identifiable VAE), 2.3. Mô hình Lọc cộng tác Học sâu & Nhân quả IViDR (Deep Causal CF), 📚 2. Cơ sở lý thuyết chi tiết (Chương I - Phần 2), 3.1. Các mô hình xây dựng:, 3.2. Kết quả đo lường định lượng vượt trội:, 3.3. Đóng gói REST API & Tích hợp vào Ứng dụng TopTop: (+4 more)

### Community 291 - "📡 3. Các Endpoints API Được Đóng Gói Trong Notebook Colab"
Cohesion: 0.17
Nodes (12): 1. `GET /health`, 🎯 1. Nguyên Tắc Cốt Lõi (Core Principles), 🏗️ 2. Kiến Trúc Vận Hành Trên Google Colab, 2. `POST /api/v1/recommend` (Đề xuất Top-K video TopTop), 📡 3. Các Endpoints API Được Đóng Gói Trong Notebook Colab, 3. `POST /api/v1/interaction` (Ghi nhận hành vi lướt video ngắn TopTop), 📋 4. Kế Hoạch Tạo Tệp Notebook Cho Colab, 4. `POST /api/v1/rerank` (+4 more)

### Community 292 - "ĐỀ CƯƠNG CHI TIẾT — PHẦN 1: THUẬT TOÁN LỌC CỘNG TÁC (COLLABORATIVE FILTERING - CF)"
Cohesion: 0.17
Nodes (11): 🎯 1. Mục tiêu & Phạm vi nghiên cứu của Phần 1, 2.1. Lọc cộng tác dựa trên bộ nhớ (Memory-based CF), 2.2. Lọc cộng tác dựa trên mô hình (Model-based Matrix Factorization), 2.3. Vấn đề Exposure Bias & Mô hình Causal Debiased CF (CDR), 📚 2. Cơ sở lý thuyết chi tiết (Chương I - Phần 1), 3.1. Các mô hình xây dựng:, 3.2. Kết quả đo lường trên tập kiểm thử độc lập (Held-Out Test):, 🔬 3. Kế hoạch triển khai & Kết quả thực nghiệm Phần 1 (Chương II - Phần 1) (+3 more)

### Community 293 - "tunnel.py"
Cohesion: 0.33
Nodes (3): publish_server_url(), main(), start_tunnel()

### Community 307 - "Giao thức benchmark v4 và phân tích sai số"
Cohesion: 0.17
Nodes (11): Coat, Các yếu tố sai số cần kiểm soát, Cách chạy và tiêu chí hoàn tất benchmark, Giao thức benchmark v4 và phân tích sai số, Giao thức đã triển khai, Giới hạn kiến trúc địa phương, KuaiRand, Kết quả kiểm tra dữ liệu và smoke test (+3 more)

### Community 308 - "presentation_slides.md"
Cohesion: 0.18
Nodes (8): Cảm ơn Thầy Cô và Các Bạn đã lắng nghe!, 💬 Mời Thầy Cô và Các Bạn Đặt Câu Hỏi (Q&A), Benchmark v4: validation và test độc lập, Candidate pool coat, Giới hạn sai số, Kết quả trên held-out test:, Chi Tiết Toàn Bộ Các Trials Trong Grid Search, KuaiRand-Pure: Báo Cáo Tinh Chỉnh Siêu Tham Số (Validation Tuning)

### Community 327 - "Nguyên nhân chênh lệch với bài báo (đo ngày 2026-10-15)"
Cohesion: 0.18
Nodes (11): Ablation MF trên Coat (10 seed 42–51, test NDCG@5), Cập nhật 2026-10-16: iDCF 2 giai đoạn và IViDR theo giai đoạn (Coat, 10 seed 42–51), IViDR trên Coat (5 seed 42–46, IV tách khỏi proxy), KuaiRand-Pure sau khi sửa (seed 42, `benchmark --dataset kuairand`), Kết quả 10 seed sau khi sửa MF (Coat, seed 42–51; IViDR ở đây là bản joint, mã đã gỡ), MF trên KuaiRand-Pure (seed 42, test), Nguyên nhân chênh lệch với bài báo (đo ngày 2026-10-15), Nguồn (+3 more)

### Community 350 - "Vì sao MF, DR-BIAS + CDR và IViDR thấp hơn bài báo"
Cohesion: 0.22
Nodes (9): Coat (10 seed 42–51, mean ± std; Wilcoxon theo cặp seed so với MF), Còn lại, DR-BIAS + CDR, IViDR, KuaiRand-Pure (seed 42, cấu hình mới; `results/benchmark_kuairand_all_2edb44f179_v4.md`), Lệnh tái lập, MF, Tóm tắt (+1 more)

### Community 395 - "📖 Hướng Dẫn Truy Cập Nhanh"
Cohesion: 0.29
Nodes (7): 1. Đề Cương & Quản Lý Đồ Án Chuyên Ngành, 2. Thuyết Trình & Bảo Vệ Đồ Án, 3. Vận Hành Kỹ Thuật & Tích Hợp API, 4. Nghiên Cứu Thực Nghiệm & Đo Lường, 🗂️ Cấu Trúc Thư Mục Chuẩn Hóa, 📖 Hướng Dẫn Truy Cập Nhanh, 📚 Trung Tâm Tài Liệu Nghiên Cứu & Kỹ Thuật (Documentation Hub)

### Community 397 - "Benchmark MF, IViDR và DR-BIAS + CDR"
Cohesion: 0.29
Nodes (7): Benchmark MF, iDCF, IViDR và DR-BIAS + CDR, Chạy MF và so sánh bộ tối ưu, Dọn dữ liệu sinh ra, Giao thức, Môi trường và dữ liệu, Một đầu chạy cho test và thí nghiệm, RecSys API cho app TopTop

### Community 431 - "Benchmark v4: validation và test độc lập"
Cohesion: 0.33
Nodes (5): Benchmark v4: validation và test độc lập, Candidate pool coat, Candidate pool kuairand, Giới hạn sai số, Kết quả trên held-out test:

### Community 432 - "Benchmark v4: validation và test độc lập"
Cohesion: 0.33
Nodes (5): Benchmark v4: validation và test độc lập, Candidate pool coat, Giới hạn sai số, Kiểm định theo cặp user (coat, test), Kết quả trên held-out test:

### Community 433 - "Benchmark v4: validation và test độc lập"
Cohesion: 0.33
Nodes (5): Benchmark v4: validation và test độc lập, Candidate pool coat, Giới hạn sai số, Kiểm định theo cặp user (coat, test), Kết quả trên held-out test:

### Community 434 - "Benchmark v4: validation và test độc lập"
Cohesion: 0.33
Nodes (5): Benchmark v4: validation và test độc lập, Candidate pool kuairand, Giới hạn sai số, Kiểm định theo cặp user (kuairand, test), Kết quả trên held-out test:

### Community 463 - "Kiểm tra MF và bộ tối ưu — 2026-09-27"
Cohesion: 0.33
Nodes (5): Kiểm tra MF và bộ tối ưu — 2026-09-27, Kết luận, Thay đổi và kiểm chứng, Đánh giá và các giới hạn, Đối chiếu nguồn gốc

## Knowledge Gaps
- **21 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `evaluate_checkpoint()` connect `test_suite.py` to `MatrixFactorization`, `format_grid_table`, `Benchmark MF, IViDR và DR-BIAS + CDR`, `CDRFramework`, `torch`, `evaluate_unbiased_model`, `test_methodology.py`?**
  _High betweenness centrality (0.262) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `evaluate_checkpoint()` (e.g. with `Dọn dữ liệu sinh ra` and `CDRFramework`) actually correct?**
  _`evaluate_checkpoint()` has 4 INFERRED edges - model-reasoned connections that need verification._
- **Should `cdr_idcf_tune.py` be split into smaller, more focused modules?**
  _Cohesion score 0.0967741935483871 - nodes in this community are weakly interconnected._
- **Why does `Benchmark MF, iDCF, IViDR và DR-BIAS + CDR` connect `Benchmark MF, IViDR và DR-BIAS + CDR` to `docs/README.md`?**
  _High betweenness centrality (0.244) - this node is a cross-community bridge._
- **Should `EngineTests` be split into smaller, more focused modules?**
  _Cohesion score 0.11052631578947368 - nodes in this community are weakly interconnected._
- **Why does `Dọn dữ liệu sinh ra` connect `Benchmark MF, IViDR và DR-BIAS + CDR` to `test_suite.py`?**
  _High betweenness centrality (0.242) - this node is a cross-community bridge._
- **Should `test_suite.py` be split into smaller, more focused modules?**
  _Cohesion score 0.060837381448068474 - nodes in this community are weakly interconnected._