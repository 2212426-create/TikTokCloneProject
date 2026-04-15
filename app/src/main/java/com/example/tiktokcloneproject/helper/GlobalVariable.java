package com.example.tiktokcloneproject.helper;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.HashMap;
import java.util.Map;

public class GlobalVariable extends Application {
    private Uri avatarUri;

    @Override
    public void onCreate() {
        super.onCreate();
        
        try {
            // Cấu hình Firestore an toàn
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
            FirebaseFirestore.getInstance().setFirestoreSettings(settings);
            
            // Cấu hình Realtime Database
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {
            Log.e("FirebaseInit", "Lỗi khởi tạo Firebase: " + e.getMessage());
        }

        // Khởi tạo Cloudinary
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dbxinpidm");
        try {
            MediaManager.init(this, config);
        } catch (Exception ignored) {}
    }

    public void setAvatarUri(Uri avatarUri) {
        this.avatarUri = avatarUri;
    }

    public Uri getAvatarUri() {
        return avatarUri;
    }
}
