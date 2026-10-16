package com.example.tiktokcloneproject.helper;

import android.app.Activity;
import android.app.Application;
import android.net.Uri;
import android.util.Log;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.cloudinary.android.MediaManager;
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GlobalVariable extends Application {
    private static final String TAG = "GlobalVariable";
    private Uri avatarUri;
    private static SimpleCache videoCache;
    private int startedActivities = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Force Dark Mode
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
        );
        
        try {
            FirebaseHelper.getDatabase().setPersistenceEnabled(true);
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
            FirebaseFirestore.getInstance().setFirestoreSettings(settings);
        } catch (Exception e) {
            Log.e(TAG, "Firebase init error: " + e.getMessage());
        }

        // Cloudinary
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dbxinpidm");
        try { MediaManager.init(this, config); } catch (Exception ignored) {}

        // ExoPlayer Cache
        if (videoCache == null) {
            LeastRecentlyUsedCacheEvictor evictor = new LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024);
            videoCache = new SimpleCache(new File(getCacheDir(), "media_cache"), evictor, new StandaloneDatabaseProvider(this));
        }

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                if (startedActivities == 0) updatePresence(true);
                startedActivities++;
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                startedActivities = Math.max(0, startedActivities - 1);
                if (startedActivities == 0 && !activity.isChangingConfigurations()) {
                    updatePresence(false);
                }
            }

            @Override public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {}
            @Override public void onActivityResumed(@NonNull Activity activity) {}
            @Override public void onActivityPaused(@NonNull Activity activity) {}
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
            @Override public void onActivityDestroyed(@NonNull Activity activity) {}
        });

        setupPresenceAutomation();
    }

    private void setupPresenceAutomation() {
        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                DatabaseReference connectedRef = FirebaseHelper.getDatabase().getReference(".info/connected");
                connectedRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean connected = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                        if (connected && startedActivities > 0) {
                            updatePresence(true);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
        });
    }

    private void updatePresence(boolean online) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        DatabaseReference statusRef = FirebaseHelper.getDatabase().getReference("status").child(currentUser.getUid());
        
        Map<String, Object> offlineStatus = new HashMap<>();
        offlineStatus.put("online", false);
        offlineStatus.put("lastActive", ServerValue.TIMESTAMP);

        if (online) {
            Map<String, Object> onlineStatus = new HashMap<>();
            onlineStatus.put("online", true);
            onlineStatus.put("lastActive", ServerValue.TIMESTAMP);

            // Rất quan trọng: Server sẽ tự set offline khi app bị kill hoặc mất mạng
            statusRef.onDisconnect().setValue(offlineStatus);
            statusRef.setValue(onlineStatus);
        } else {
            statusRef.setValue(offlineStatus);
        }
    }

    public static SimpleCache getVideoCache() { return videoCache; }
    public void setAvatarUri(Uri avatarUri) { this.avatarUri = avatarUri; }
    public Uri getAvatarUri() { return avatarUri; }
}
