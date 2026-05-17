# Thiết kế Cơ sở dữ liệu Firebase (Firestore)

Dưới đây là cấu trúc đề xuất cho Cloud Firestore, được thiết kế dưới dạng NoSQL, phù hợp cho cả App Android và Web Admin.

## 1. Collection: `users`
Lưu trữ thông tin của tất cả người dùng (App) và nhân sự quản trị (Web). Đồng bộ với class `User.java` của App Android.

- **Document ID:** `userId` (Lấy từ Firebase Authentication)
- **Fields:**
  - `userId`: string
  - `username`: string
  - `avatarUrl`: string (Đã chuẩn hóa - trước đây có thể là avatarUri hoặc avatarName)
  - `email`: string
  - `phone`: string
  - `birthdate`: string
  - `isPrivate`: boolean
  - `followers`: number
  - `following`: number
  - `likes`: number
  - **Dữ liệu mở rộng cho Admin Web:**
    - `role`: string (Enum: `"admin"`, `"moderator"`, `"user"`) - *Mặc định là "user".*
    - `status`: string (Enum: `"active"`, `"banned"`, `"warned"`)
    - `createdAt`: timestamp

## 2. Collection: `videos`
Lưu trữ thông tin các video do người dùng đăng tải. Đồng bộ với class `Video.java` của App Android.

- **Document ID:** `videoId` (Auto-generated ID)
- **Fields:**
  - `videoId`: string
  - `videoUri`: string (Link Firebase Storage / S3)
  - `authorId`: string (Reference tới `users.userId`)
  - `username`: string (Tên người đăng lúc up video)
  - `description`: string
  - `timestamp`: number (Kiểu long)
  - `totalLikes`: number
  - `totalComments`: number
  - `watchCount`: number
  - **Dữ liệu kiểm duyệt (Dành cho Admin Web):**
    - `moderationStatus`: string (Enum: `"pending"`, `"approved"`, `"rejected"`)
    - `aiFlagged`: boolean (Đánh dấu nếu AI phát hiện bất thường)
    - `aiConfidence`: number (0-100, tỉ lệ % AI nghi ngờ vi phạm)
    - `rejectedReason`: string (Nếu bị reject, ghi lý do tại đây)
    - `reviewedBy`: string (Reference tới `userId` của Admin/Moderator đã duyệt)

## 3. Collection: `reports`
Lưu trữ các báo cáo vi phạm từ người dùng đối với video hoặc người dùng khác.

- **Document ID:** Auto-generated ID
- **Fields:**
  - `reporterId`: string (Reference tới `users.userId` của người gửi báo cáo)
  - `targetType`: string (Enum: `"video"`, `"user"`, `"comment"`)
  - `targetId`: string (Reference tới ID của video, user hoặc comment bị báo cáo)
  - `reason`: string (Ví dụ: "Bạo lực", "Spam", "Khỏa thân",...)
  - `details`: string (Chi tiết mô tả thêm)
  - `status`: string (Enum: `"pending"`, `"resolved"`, `"dismissed"`)
  - `createdAt`: timestamp
  - `handledBy`: string (Reference tới `userId` của Admin/Moderator xử lý báo cáo)

## 4. Collection: `audit_logs`
(Dành riêng cho Web Admin) Ghi lại lịch sử thao tác của Admin/Moderator.

- **Document ID:** Auto-generated ID
- **Fields:**
  - `adminId`: string (Reference tới `users.userId` của Admin/Moderator)
  - `action`: string (Ví dụ: `"BAN_USER"`, `"APPROVE_VIDEO"`, `"REJECT_VIDEO"`, `"CHANGE_ROLE"`)
  - `targetId`: string (ID của đối tượng bị tác động)
  - `details`: map (Chứa thông tin thay đổi cụ thể. VD: `{ "oldStatus": "active", "newStatus": "banned", "reason": "Spam" }`)
  - `createdAt`: timestamp

## 5. Security Rules (Cơ bản dự kiến)
- `users`: User chỉ sửa được thông tin cá nhân của mình. `admin` có quyền đọc/ghi mọi documents. `moderator` có quyền đọc.
- `videos`: Mọi người được đọc `video` có status `approved`. User tự đăng video thì tạo mới (status default `pending`). `admin` và `moderator` có quyền đọc/sửa tất cả (để duyệt/xóa).
- `audit_logs`: Chỉ `admin` mới có quyền đọc (để kiểm tra hoạt động hệ thống). Hệ thống server (Admin SDK) ghi log.

---
*Lưu ý: Bạn sẽ cần thiết lập Firebase Admin SDK trên Backend hoặc gọi trực tiếp Firestore từ ReactJS (nhưng phải thiết lập Security Rules cực kỳ chặt chẽ).*