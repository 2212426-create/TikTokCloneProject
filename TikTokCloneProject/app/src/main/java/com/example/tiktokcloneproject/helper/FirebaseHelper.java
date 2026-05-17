package com.example.tiktokcloneproject.helper;

import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHelper {
    public static FirebaseDatabase getDatabase() {
        // Sử dụng URL chính xác từ Firebase Console của bạn
        return FirebaseDatabase.getInstance("https://mytiktokclone-f9789-default-rtdb.firebaseio.com/");
    }
}
