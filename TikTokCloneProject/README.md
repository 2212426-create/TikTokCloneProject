# 📱 TopTop Android Application (TikTok Clone Client)

Đây là mã nguồn ứng dụng di động **TopTop** (TikTok Clone client) chạy native trên nền tảng Android. Ứng dụng tích hợp trình phát video chất lượng cao, các tính năng tương tác mạng xã hội thời gian thực và trí tuệ nhân tạo **Gemini AI** để tối ưu hóa trải nghiệm người dùng.

> [!NOTE]
> Để xem chi tiết về kiến trúc toàn hệ thống bao gồm cả Web App quản trị và cơ sở dữ liệu Cloud Firestore, vui lòng tham khảo [Tài liệu tổng quan dự án TopTop ở thư mục gốc](../README.md).

---

## 🛠️ Công Nghệ & Thư Viện Sử Dụng (Tech Stack)

*   **Ngôn ngữ lập trình:** Java 17
*   **Android SDK:** Compile SDK `34`, Min SDK `24`
*   **Build System:** Gradle (AGP 8.x)
*   **Phát Video:** `ExoPlayer` (v2.19.0) - Trình phát media tối ưu hiệu suất, tiết kiệm băng thông và hỗ trợ caching dữ liệu.
*   **Media Cloud:** `Cloudinary` - Upload, quản lý và phân phối video/ảnh đại diện chất lượng cao.
*   **Trí Tuệ Nhân Tạo:** `Google Generative AI` (v0.9.0) - Gọi mô hình AI `gemini-3-flash-preview` trực tiếp trên di động để gợi ý hashtag viral.
*   **Backend & DB:** `Firebase` (Auth, Cloud Firestore, Realtime Database, Cloud Storage).
*   **Đọc Ảnh:** `Glide` (v4.15.1) - Tải và bộ nhớ đệm hình ảnh.

---

## ✨ Các Tính Năng Đã Triển Khai

1.  **Feed Video Ngắn:** Giao diện vuốt dọc xem video vô tận, tự động phát/lặp lại video cực kỳ mượt mà.
2.  **Tạo & Đăng Tải Video (Camera):** Quay phim trực tiếp qua camera thiết bị hoặc chọn video có sẵn từ thư viện, nén và upload lên Cloudinary.
3.  **Tự động Gợi ý Hashtag bằng Gemini AI:** Phân tích hình ảnh thumbnail của video và tự động đề xuất 5 - 10 hashtag viral bằng tiếng Việt (`#xuhuong`, `#trending`...).
4.  **Tương tác Video:** Thích video (Like), Bình luận dưới video (Comment), và chia sẻ liên kết tài khoản.
5.  **Mạng xã hội:** Follow/Unfollow người dùng khác, xem danh sách người theo dõi & người đang theo dõi.
6.  **Nhắn Tin Trực Tiếp (Direct Chat):** Hệ thống nhắn tin real-time giữa các cặp người dùng.
7.  **Quản lý Cá Nhân:** Thay đổi ảnh đại diện (avatar), cập nhật tiểu sử (bio), đổi mật khẩu hoặc xóa tài khoản.
8.  **Tìm Kiếm:** Tìm kiếm người dùng, bài đăng hoặc hashtag.

---

## 📂 Cấu Trúc Mã Nguồn (Directory Structure)

```text
TikTokCloneProject/
├── app/
│   ├── build.gradle                # Khai báo thư viện và cấu hình build app
│   ├── google-services.json        # File cấu hình kết nối Firebase (Cần bổ sung)
│   └── src/main/
│       ├── AndroidManifest.xml     # Đăng ký quyền (Camera, Internet, Storage) và các Activity
│       ├── java/com/example/tiktokcloneproject/
│       │   ├── activity/           # Các màn hình điều hướng của ứng dụng
│       │   │   ├── HomeScreenActivity.java        # Trang chủ chứa các Fragment
│       │   │   ├── CameraActivity.java            # Quay video bằng Camera
│       │   │   ├── DescriptionVideoActivity.java  # Thêm mô tả và dùng Gemini AI gợi ý Hashtag
│       │   │   ├── ChatActivity.java              # Nhắn tin thời gian thực
│       │   │   └── ...
│       │   ├── adapters/           # Cầu nối dữ liệu hiển thị danh sách (VideoAdapter, CommentAdapter...)
│       │   ├── fragment/           # Các Fragment tab chính (ProfileFragment, SearchFragment, VideoFragment...)
│       │   ├── helper/             # Các lớp tiện ích xử lý logic phụ trợ
│       │   │   ├── GeminiHelper.java             # Quản lý kết nối & Prompt gửi tới Gemini AI
│       │   │   └── LegacyHashtagFixer.java
│       │   └── model/              # Các đối tượng dữ liệu (User.java, Video.java, Comment.java...)
│       └── res/                    # Thiết kế giao diện XML, hình ảnh tĩnh, màu sắc
│
├── build.gradle                    # Gradle mức dự án
├── settings.gradle                 # Cấu hình module dự án
└── local.properties                # File chứa các thiết lập cục bộ (Chứa GEMINI_API_KEY)
```

---

## 🚀 Hướng Dẫn Cài Đặt & Chạy Thử (Setup Guide)

1.  **Clone repository** về máy tính cá nhân.
2.  Mở ứng dụng **Android Studio** (Phiên bản Jellyfish hoặc mới hơn).
3.  Chọn **File > Open** và chọn thư mục `TikTokCloneProject`.
4.  Cài đặt Firebase:
    *   Tải file `google-services.json` từ dự án Firebase của bạn.
    *   Đặt file này vào thư mục: `TikTokCloneProject/app/`.
5.  Cấu hình khóa bảo mật **Gemini API Key**:
    *   Mở file `local.properties` ở thư mục gốc của Android project (nếu chưa có, hãy tạo mới).
    *   Thêm dòng sau và thay thế bằng API key của bạn:
        ```properties
        GEMINI_API_KEY=AIzaSy...YourActualGeminiApiKey
        ```
6.  Đồng bộ hóa Gradle (**Sync Project with Gradle Files**).
7.  Cắm điện thoại Android của bạn (đã bật *Chế độ nhà phát triển* và *Gỡ lỗi USB*) hoặc khởi chạy máy ảo (Emulator).
8.  Nhấn nút **Run** (`Shift + F10`) để build và cài đặt ứng dụng.
