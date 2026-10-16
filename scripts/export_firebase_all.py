# -*- coding: utf-8 -*-
"""
Script xuất toàn bộ dữ liệu từ Firebase của TopTop:
1. Cloud Firestore (videos, users, profiles, user_interests, comments, reports)
2. Realtime Database (chats, notifications, status, likes...)
3. Firebase Authentication (danh sách tài khoản UIDs, emails, profile...)

Tự động tìm kiếm file Service Account Key trong:
- Thư mục hiện tại: D:\\University\\Mobile_Application\\TopTop
- Thư mục Downloads: C:\\Users\\phung\\Downloads
"""

import os
import sys
import json
from pathlib import Path

# Cấu hình dự án TopTop
PROJECT_ID = "mytiktokclone-f9789"
RTDB_URL = "https://mytiktokclone-f9789-default-rtdb.firebaseio.com/"

BASE_DIR = Path(__file__).resolve().parents[1]  # gốc repo (script nằm trong scripts/)
DOWNLOADS_DIR = Path(os.environ.get("USERPROFILE", "C:/Users/phung")) / "Downloads"
OUTPUT_DIR = BASE_DIR / "data" / "firebase_export"

FIRESTORE_COLLECTIONS = [
    "videos",
    "users",
    "profiles",
    "user_interests",
    "comments",
    "reports"
]


def find_service_account_key():
    """Tự động tìm file private key trong thư mục dự án hoặc thư mục Downloads."""
    candidates = []
    # 1. Tìm trong thư mục dự án
    for p in BASE_DIR.glob("*.json"):
        candidates.append(p)
    # 2. Tìm trong Downloads
    if DOWNLOADS_DIR.exists():
        for p in DOWNLOADS_DIR.glob("*adminsdk*.json"):
            candidates.append(p)
        for p in DOWNLOADS_DIR.glob("*serviceAccountKey*.json"):
            candidates.append(p)

    for p in candidates:
        if p.name.startswith("mytiktokclone") or "adminsdk" in p.name.lower() or "serviceaccount" in p.name.lower():
            try:
                with open(p, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    if data.get("type") == "service_account" and data.get("project_id") == PROJECT_ID:
                        return p
            except Exception:
                continue
    return None


def main():
    print("=" * 65)
    print("🚀 BẮT ĐẦU XUẤT TỰ ĐỘNG FIREBASE TOPTOP (FIRESTORE, RTDB, AUTH)")
    print("=" * 65)

    key_file = find_service_account_key()
    if not key_file:
        print("\n❌ CHƯA TÌM THẤY FILE PRIVATE KEY DỰ ÁN!")
        print("\n👉 CÁCH LẤY FILE KEY CHỈ TRONG 10 GIÂY:")
        print("1. Mở Firebase Console: https://console.firebase.google.com/project/mytiktokclone-f9789/settings/serviceaccounts/adminsdk")
        print("2. Chọn tab 'Service accounts' (Tài khoản dịch vụ)")
        print("3. Bấm nút màu xanh: [Generate new private key] (Tạo khóa riêng tư mới)")
        print(f"4. File JSON sẽ được tải về thư mục Downloads. Bạn chỉ cần để nguyên đó hoặc copy vào đây, rồi chạy lại script này là xong ngay!\n")
        sys.exit(1)

    print(f"🔑 Đã tìm thấy khóa xác thực: {key_file.name}")

    try:
        import firebase_admin
        from firebase_admin import credentials, firestore, db, auth
    except ImportError:
        print("⚠️ Chưa có thư viện 'firebase-admin'. Đang tự động cài đặt...")
        import subprocess
        subprocess.check_call([sys.executable, "-m", "pip", "install", "firebase-admin"])
        import firebase_admin
        from firebase_admin import credentials, firestore, db, auth

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    print("\n🔗 Đang khởi tạo kết nối Firebase...")
    cred = credentials.Certificate(str(key_file))
    try:
        firebase_admin.get_app()
    except ValueError:
        firebase_admin.initialize_app(cred, {
            "databaseURL": RTDB_URL,
            "projectId": PROJECT_ID
        })
    print("✅ Kết nối Firebase thành công!")

    # 1. FIRESTORE
    print("\n📦 [1/3] Đang xuất Cloud Firestore...")
    fs_client = firestore.client()
    firestore_data = {}
    for col in FIRESTORE_COLLECTIONS:
        try:
            docs = fs_client.collection(col).stream()
            col_list = []
            for doc in docs:
                data = doc.to_dict() or {}
                data["_doc_id"] = doc.id
                col_list.append(data)
            firestore_data[col] = col_list
            print(f"   ✓ Collection '{col}': {len(col_list):,} documents")
        except Exception as e:
            print(f"   ⚠️ Lỗi collection '{col}': {e}")

    fs_path = OUTPUT_DIR / "firestore_data.json"
    with open(fs_path, "w", encoding="utf-8") as f:
        json.dump(firestore_data, f, ensure_ascii=False, indent=2, default=str)
    print(f"💾 Đã lưu Firestore tại: {fs_path}")

    # 2. REALTIME DATABASE
    print("\n⚡ [2/3] Đang xuất Realtime Database...")
    try:
        rtdb_ref = db.reference("/")
        rtdb_data = rtdb_ref.get() or {}
        rtdb_path = OUTPUT_DIR / "realtime_db_data.json"
        with open(rtdb_path, "w", encoding="utf-8") as f:
            json.dump(rtdb_data, f, ensure_ascii=False, indent=2, default=str)
        print(f"   ✓ Đã xuất cây dữ liệu RTDB ({len(rtdb_data.keys())} root keys)")
        print(f"💾 Đã lưu Realtime Database tại: {rtdb_path}")
    except Exception as e:
        print(f"   ⚠️ Lỗi Realtime DB: {e}")

    # 3. AUTHENTICATION
    print("\n👥 [3/3] Đang xuất Firebase Authentication...")
    try:
        users_list = []
        page = auth.list_users()
        while page:
            for user in page.users:
                users_list.append({
                    "uid": user.uid,
                    "email": user.email,
                    "display_name": user.display_name,
                    "phone_number": user.phone_number,
                    "disabled": user.disabled,
                    "photo_url": user.photo_url,
                    "created_at": user.user_metadata.creation_timestamp,
                    "last_sign_in": user.user_metadata.last_sign_in_timestamp
                })
            page = page.get_next_page()

        auth_path = OUTPUT_DIR / "auth_users_data.json"
        with open(auth_path, "w", encoding="utf-8") as f:
            json.dump(users_list, f, ensure_ascii=False, indent=2, default=str)
        print(f"   ✓ Đã xuất danh sách {len(users_list):,} người dùng")
        print(f"💾 Đã lưu Authentication tại: {auth_path}")
    except Exception as e:
        print(f"   ⚠️ Lỗi Authentication: {e}")

    print("\n" + "=" * 65)
    print(f"🎉 HOÀN TẤT XUẤT CẢ 3 DỊCH VỤ! Thư mục lưu: {OUTPUT_DIR}")
    print("=" * 65)


if __name__ == "__main__":
    main()
