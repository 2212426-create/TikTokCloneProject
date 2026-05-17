# TopTop Management Web App

## Giới thiệu
Đây là trang web quản trị (Admin Dashboard) dành cho ứng dụng TopTop (TikTok Clone trên Android). Hệ thống cung cấp các công cụ để ban quản trị có thể dễ dàng quản lý người dùng và kiểm duyệt nội dung video, đảm bảo môi trường cộng đồng an toàn, tuân thủ tiêu chuẩn cộng đồng của ứng dụng.

## Tính năng chính

### 1. Quản lý Người dùng (User Management)
- Xem danh sách toàn bộ người dùng đã đăng ký trên hệ thống.
- Tra cứu thông tin chi tiết, lịch sử đăng tải video của người dùng.
- Thao tác khóa (ban), cảnh cáo hoặc mở khóa tài khoản đối với các người dùng có dấu hiệu vi phạm.

### 2. Kiểm duyệt Video (Video Moderation)
- **Kiểm duyệt tự động bằng AI:** Tích hợp các mô hình Trí tuệ Nhân tạo (AI) / Machine Learning (ví dụ: Google Cloud Video Intelligence, AWS Rekognition) để tự động phân tích và ngăn chặn ngay lập tức các video chứa nội dung đồi trụy, cờ bạc, bạo lực hoặc vi phạm tiêu chuẩn cộng đồng trước khi chúng được hiển thị cho người dùng.
- **Xét duyệt thủ công:** Ban quản trị có thể xem lại các video bị AI đánh dấu nghi ngờ (flagged) hoặc các video bị người dùng báo cáo để đưa ra quyết định cuối cùng.
- **Xử lý vi phạm:** Quyết định phê duyệt để video hiển thị công khai hoặc gỡ bỏ (reject) và áp dụng hình phạt với tài khoản vi phạm.
- **Quản lý báo cáo (Report):** Nhận và xử lý các video bị báo cáo từ người dùng trên app Android.

### 3. Thống kê & Báo cáo (Analytics & Dashboard)
- **Tổng quan hệ thống:** Biểu đồ hiển thị số lượng người dùng mới, lượng video tải lên hàng ngày/tuần/tháng.
- **Thống kê tương tác:** Theo dõi tổng lượt xem (views), lượt thích (likes), bình luận (comments) và chia sẻ (shares) trên toàn bộ nền tảng.
- **Thống kê Doanh thu (Monetization):** (Tùy chọn) Báo cáo thu nhập từ các chiến dịch quảng cáo, quà tặng ảo trong livestream.
- **Hiệu suất AI:** Thống kê tỉ lệ video bị AI chặn tự động so với lượng video an toàn, giúp đánh giá và tinh chỉnh mô hình AI.

### 4. Quản lý Hệ thống (System Configuration)
- **Nhật ký hoạt động (Audit Logs):** Ghi lại mọi thao tác của Admin trên hệ thống (ví dụ: ai đã khóa tài khoản nào, lúc mấy giờ) để đảm bảo tính minh bạch.
- **Phân quyền Hệ thống:** Hệ thống phân chia quyền hạn rõ ràng để đảm bảo an toàn:
  - **Admin:** Quản trị viên cấp cao có toàn quyền kiểm soát hệ thống, thiết lập cài đặt và quản lý nhân sự.
  - **Moderator (Kiểm duyệt viên):** Nhân viên chuyên trách việc xem xét, duyệt video nội dung và xử lý các báo cáo vi phạm từ người dùng (không có quyền can thiệp vào cài đặt hệ thống).
  - **User:** Người dùng thông thường sử dụng ứng dụng di động.

## Công nghệ đề xuất (Tech Stack)
*(Có thể thay đổi tùy theo quyết định thực tế)*
- **Frontend:** React.js kết hợp với TailwindCSS.
- **Backend:** Node.js (Express.js).
- **Cơ sở dữ liệu:** Firebase Firestore / Realtime Database (đồng bộ với app Android).

## Cấu trúc thư mục Frontend dự kiến
```text
TikTokManagement/
├── public/                 # Các tài nguyên tĩnh (images, favicon, index.html)
├── src/
│   ├── assets/             # Hình ảnh, icons, fonts được import vào code
│   ├── components/         # Các UI component dùng chung (Button, Modal, Table, Sidebar...)
│   ├── layouts/            # Layout chính của ứng dụng (AdminLayout, AuthLayout...)
│   ├── pages/              # Các trang giao diện chính
│   │   ├── Dashboard/      # Trang thống kê tổng quan
│   │   ├── Users/          # Quản lý người dùng
│   │   ├── Moderation/     # Xét duyệt video (có list và chi tiết video)
│   │   └── Settings/       # Cài đặt hệ thống, phân quyền
│   ├── routes/             # Cấu hình routing (React Router) và phân quyền truy cập
│   ├── services/           # Gọi API, tương tác với Firebase/Backend
│   ├── store/              # Quản lý state toàn cục (Redux / Zustand / Context API)
│   ├── utils/              # Các hàm tiện ích (format ngày tháng, xử lý chuỗi...)
│   ├── App.js              # Component gốc
│   └── index.js            # Điểm khởi chạy ứng dụng (Entry point)
├── .env                    # Biến môi trường
├── package.json            # Quản lý thư viện phụ thuộc
└── README.md
```

## Hướng dẫn cài đặt (Chạy ở Local)
*(Sẽ được cập nhật chi tiết sau khi khởi tạo mã nguồn)*

1. Clone repository này về máy.
2. Di chuyển vào thư mục dự án và cài đặt thư viện: 
   ```bash
   npm install
   # hoặc
   yarn install
   ```
3. Tạo file `.env` và thiết lập các biến môi trường cần thiết (ví dụ: thông tin kết nối Firebase/Database).
4. Khởi chạy ứng dụng:
   ```bash
   npm start
   # hoặc
   yarn dev
   ```

---
*Dự án Web App quản trị cho hệ sinh thái TopTop (TikTok Clone).*