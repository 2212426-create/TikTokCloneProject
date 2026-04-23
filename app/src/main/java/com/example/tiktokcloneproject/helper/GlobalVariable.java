package com.example.tiktokcloneproject.helper;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GlobalVariable extends Application {
    private Uri avatarUri;
    private static SimpleCache videoCache;

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

        // Khởi tạo SimpleCache cho ExoPlayer
        if (videoCache == null) {
            LeastRecentlyUsedCacheEvictor evictor = new LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024); // 100MB
            videoCache = new SimpleCache(new File(getCacheDir(), "media_cache"), evictor, new StandaloneDatabaseProvider(this));
        }
    }

    public static SimpleCache getVideoCache() {
        return videoCache;
    }

    public void setAvatarUri(Uri avatarUri) {
        this.avatarUri = avatarUri;
    }

    public Uri getAvatarUri() {
        return avatarUri;
    }
}
