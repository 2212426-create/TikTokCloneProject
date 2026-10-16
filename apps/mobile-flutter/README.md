# TopTop Flutter

Ứng dụng video ngắn TopTop, hỗ trợ QA trên Web, Android và iOS.

## Chạy cục bộ

```bash
flutter pub get
flutter run -d web-server --web-port=5000
flutter run -d <android-device-id>
```

Sau khi chạy lệnh Web, mở `http://localhost:5000` bằng Edge thông thường. Trong VS Code, chọn launch profile **TopTop Web (localhost:5000)** rồi nhấn `F5` để chạy Chrome debug ở cùng port.

Để dùng profile Microsoft Edge thông thường (không InPrivate/không profile debug tạm) từ VS Code, chọn profile **TopTop Web Server (Edge thường, localhost:5000)**, rồi mở `http://localhost:5000` bằng Edge.

Muốn tự mở trang theo browser mặc định, chạy VS Code task **TopTop Web: open browser tab (localhost:5000)**. Task ưu tiên gửi URL vào cửa sổ browser đang chạy; nếu chưa có browser, Windows sẽ mở một cửa sổ/tab mới.

Để build kiểm tra:

```bash
flutter test
flutter analyze
flutter build web
flutter build apk --debug
```

## Hành vi theo nền tảng

- **Web:** giao diện mobile-first, tối đa 600 px; chọn video từ máy và upload avatar/video được hỗ trợ. Browser không hiển thị nút quay camera.
- **Android/iOS:** có thể chọn video từ thư viện hoặc quay video bằng camera; app chỉ hỗ trợ màn hình dọc.
- Video được upload lên Cloudinary; avatar được lưu trong Firebase Storage.

## Firebase và Google Sign-In

Ứng dụng dùng Firebase project `mytiktokclone-f9789`. Các giá trị Firebase trong source chỉ phục vụ nhận diện app; trước khi QA Google Sign-In, người quản trị project phải hoàn tất các cấu hình sau:

1. Firebase Authentication: bật Email/Password và Google.
2. Google Cloud OAuth: thêm SHA-1 và SHA-256 của app Android `com.example.toptop_flutter`; sau đó tải lại `android/app/google-services.json`.
3. Firebase: đăng ký iOS bundle ID `com.example.toptopFlutter`, tải `GoogleService-Info.plist` và thêm vào target `Runner` trong Xcode. URL scheme và `GIDClientID` hiện được lấy từ iOS client đã có trong `firebase_options.dart`.
4. Firebase Authentication: thêm domain triển khai Web vào **Authorized domains**. Web dùng Firebase `signInWithPopup`; nếu Google báo **Authorization Error**, mở Google Cloud Console → Credentials → OAuth 2.0 Web client của Firebase và thêm chính xác origin đang chạy (ví dụ `http://localhost:5000` hoặc domain HTTPS production) vào **Authorized JavaScript origins**.

## Checklist QA

- Email/password và Google Sign-In trên Chrome, Android thật và iPhone thật.
- Từ chối/cho phép quyền camera, micro và thư viện trên mobile.
- Hủy chọn media, chọn tệp không hợp lệ hoặc lớn hơn 100 MB, upload avatar và video, xem preview rồi phát lại trong feed.
- Kiểm tra mất mạng và thử lại upload; app phải hiển thị lỗi tiếng Việt, không crash.
