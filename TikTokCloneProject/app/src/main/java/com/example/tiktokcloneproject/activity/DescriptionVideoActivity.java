package com.example.tiktokcloneproject.activity;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.helper.GeminiHelper;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DescriptionVideoActivity extends FragmentActivity implements View.OnClickListener {
    EditText edtDescription;
    Button btnDescription, btnAddHashtag;
    ImageView imvShortCutVideo, btnBack;
    ImageButton btnAiSuggest;
    ProgressBar pbAiLoading;

    final String REGEX_HASHTAG = "#([A-Za-z0-9_-]+)";
    String username = "user";
    Uri videoUri;
    final long maximumDuration = 300000;

    FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseFirestore db;

    ArrayList<String> hashtags;
    String Id;
    final String TAG = "DescriptionVideoActivity";
    Bitmap thumbnail;

    NotificationManagerCompat mNotifyManager;
    NotificationCompat.Builder mBuilder;
    private static final int NOTIFICATION_ID = 4004;
    private static final String UPLOAD_PRESET = "toptopclone";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description_video);

        edtDescription = findViewById(R.id.edtDescription);
        btnDescription = findViewById(R.id.btnDescription);
        imvShortCutVideo = findViewById(R.id.imvShortCutVideo);
        btnBack = findViewById(R.id.btnBack);
        btnAiSuggest = findViewById(R.id.btnAiSuggest);
        pbAiLoading = findViewById(R.id.pbAiLoading);
        btnAddHashtag = findViewById(R.id.btnAddHashtag);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getData() != null) {
                videoUri = intent.getData();
            } else if (intent.getExtras() != null) {
                String videoPath = intent.getExtras().getString("videoUri");
                if (videoPath != null) {
                    videoUri = Uri.parse(videoPath);
                }
            }
            if (videoUri != null) {
                processVideoMetadata();
            }
        }
        
        hashtags = new ArrayList<>();
        createNotificationChannel();
        
        mNotifyManager = NotificationManagerCompat.from(getApplicationContext());
        mBuilder = new NotificationCompat.Builder(getApplicationContext(), "Video_Upload_Channel")
                .setContentTitle("Trạng thái tải lên")
                .setContentText("Đang chuẩn bị...")
                .setSmallIcon(R.drawable.ic_download)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true);

        btnDescription.setOnClickListener(this);
        btnAiSuggest.setOnClickListener(this);
        if (btnAddHashtag != null) btnAddHashtag.setOnClickListener(this);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void safeNotify() {
        try {
            Context appCtx = getApplicationContext();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(appCtx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
                }
            } else {
                mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "Notification error: " + e.getMessage());
        }
    }

    private void processVideoMetadata() {
        new Thread(() -> {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                if (videoUri != null && "file".equals(videoUri.getScheme())) {
                    mmr.setDataSource(videoUri.getPath());
                } else {
                    mmr.setDataSource(this, videoUri);
                }
                String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long duration = Long.parseLong(durationStr);
                
                if (duration > maximumDuration) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Video quá dài! Tối đa 5 phút.", Toast.LENGTH_LONG).show();
                        btnDescription.setEnabled(false);
                    });
                }

                thumbnail = mmr.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                runOnUiThread(() -> {
                    if (thumbnail != null && !isFinishing()) {
                        imvShortCutVideo.setImageBitmap(thumbnail);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Metadata error: " + e.getMessage());
            } finally {
                try { mmr.release(); } catch (IOException ignored) {}
            }
        }).start();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("Video_Upload_Channel", "Video Upload", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.btnDescription) {
            handlePostVideo();
        } else if (viewId == R.id.btnAiSuggest) {
            handleAiSuggestion();
        } else if (viewId == R.id.btnAddHashtag) {
            insertHashtagSymbol();
        }
    }

    private void insertHashtagSymbol() {
        String currentText = edtDescription.getText().toString();
        int cursorPosition = edtDescription.getSelectionStart();
        String hashtag = "#";
        
        // Thêm dấu cách trước # nếu chưa có và không phải ở đầu dòng
        if (cursorPosition > 0 && currentText.charAt(cursorPosition - 1) != ' ') {
            hashtag = " #";
        }
        
        edtDescription.getText().insert(cursorPosition, hashtag);
        edtDescription.requestFocus();
    }

    private void handleAiSuggestion() {
        if (thumbnail == null) {
            Toast.makeText(this, "Vui lòng đợi video tải xong", Toast.LENGTH_SHORT).show();
            return;
        }

        runOnUiThread(() -> {
            btnAiSuggest.setVisibility(View.GONE);
            pbAiLoading.setVisibility(View.VISIBLE);
            Toast.makeText(this, "AI đang tìm hashtag viral...", Toast.LENGTH_SHORT).show();
        });

        ListenableFuture<GenerateContentResponse> future = GeminiHelper.suggestHashtags(thumbnail);
        
        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> {
                    String suggestion = result.getText();
                    if (suggestion != null) {
                        String currentText = edtDescription.getText().toString();
                        if (!currentText.isEmpty() && !currentText.endsWith(" ")) {
                            currentText += " ";
                        }
                        edtDescription.setText(currentText + suggestion);
                        edtDescription.setSelection(edtDescription.getText().length());
                    }
                    pbAiLoading.setVisibility(View.GONE);
                    btnAiSuggest.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    Log.e(TAG, "AI Error: " + t.getMessage());
                    pbAiLoading.setVisibility(View.GONE);
                    btnAiSuggest.setVisibility(View.VISIBLE);
                    Toast.makeText(DescriptionVideoActivity.this, "Không thể lấy gợi ý từ AI", Toast.LENGTH_SHORT).show();
                });
            }
        }, Executors.newSingleThreadExecutor());
    }

    private void handlePostVideo() {
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập trước", Toast.LENGTH_SHORT).show();
            return;
        }

        final String description = edtDescription.getText().toString().trim();
        hashtags.clear();
        Matcher matcher = Pattern.compile(REGEX_HASHTAG).matcher(description);
        while (matcher.find()) hashtags.add(matcher.group(1).toLowerCase());
        
        Id = String.valueOf(System.currentTimeMillis());
        btnDescription.setEnabled(false);
        
        db.collection("profiles").document(user.getUid()).get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String u = task.getResult().getString("username");
                    if (u != null) username = u;
                }
                startCloudinaryUpload(description);
            });
    }

    private void startCloudinaryUpload(String description) {
        final Context appCtx = getApplicationContext();
        final String currentUid = (user != null) ? user.getUid() : "";
        final String finalUsername = username;
        
        new Thread(() -> {
            File tempFile = new File(getCacheDir(), "upload_video_" + Id + ".mp4");
            InputStream is = null;
            try {
                if (videoUri != null && "file".equals(videoUri.getScheme())) {
                    is = new java.io.FileInputStream(new File(videoUri.getPath()));
                } else if (videoUri != null) {
                    is = getContentResolver().openInputStream(videoUri);
                }

                if (is == null) {
                    throw new IOException("Cannot open input stream for video URI: " + videoUri);
                }

                try (OutputStream os = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = is.read(buffer)) > 0) os.write(buffer, 0, length);
                } finally {
                    try { is.close(); } catch (Exception ignored) {}
                }

                if (tempFile.length() > 100 * 1024 * 1024) {
                    runOnUiThread(() -> {
                        Toast.makeText(appCtx, "File quá lớn (>100MB)", Toast.LENGTH_LONG).show();
                        btnDescription.setEnabled(true);
                    });
                    tempFile.delete();
                    return;
                }

                runOnUiThread(() -> {
                    Toast.makeText(appCtx, "Đang bắt đầu tải lên...", Toast.LENGTH_SHORT).show();
                    finish(); 
                });

                MediaManager.get().upload(Uri.fromFile(tempFile))
                        .unsigned(UPLOAD_PRESET)
                        .option("resource_type", "video")
                        .callback(new UploadCallback() {
                            @Override
                            public void onStart(String requestId) {
                                mBuilder.setContentText("Đang tải video lên Cloudinary...");
                                safeNotify();
                            }

                            @Override
                            public void onProgress(String requestId, long bytes, long totalBytes) {
                                int progress = (int) (100.0 * bytes / totalBytes);
                                mBuilder.setProgress(100, progress, false).setContentText("Đang tải: " + progress + "%");
                                safeNotify();
                            }

                            @Override
                            public void onSuccess(String requestId, Map resultData) {
                                String videoUrl = (String) resultData.get("secure_url");
                                saveDataToFirestore(videoUrl, description, currentUid, finalUsername);
                                tempFile.delete(); 
                                
                                mBuilder.setContentTitle("Tải lên thành công")
                                        .setContentText("Video của bạn đã sẵn sàng!")
                                        .setProgress(0, 0, false)
                                        .setOngoing(false);
                                safeNotify();
                            }

                            @Override
                            public void onError(String requestId, ErrorInfo error) {
                                tempFile.delete();
                                String desc = error != null ? error.getDescription() : "Lỗi không xác định";
                                mBuilder.setContentTitle("Tải lên thất bại")
                                        .setContentText(desc)
                                        .setProgress(0, 0, false)
                                        .setOngoing(false);
                                safeNotify();
                            }

                            @Override public void onReschedule(String requestId, ErrorInfo error) {}
                        }).dispatch();

            } catch (Exception e) {
                Log.e(TAG, "Upload error: " + e.getMessage());
                runOnUiThread(() -> btnDescription.setEnabled(true));
            }
        }).start();
    }

    private void saveDataToFirestore(String videoUrl, String description, String uid, String uName) {
        if (uid.isEmpty()) return;
        
        Map<String, Object> videoData = new HashMap<>();
        videoUrl = videoUrl != null ? videoUrl : "";
        
        videoData.put("videoId", Id);
        videoData.put("videoUri", videoUrl);
        videoData.put("authorId", uid);
        videoData.put("username", uName);
        videoData.put("description", description);
        videoData.put("totalLikes", 0);
        videoData.put("totalComments", 0);
        videoData.put("watchCount", 0);
        videoData.put("timestamp", System.currentTimeMillis());
        videoData.put("moderationStatus", "pending"); // Chờ duyệt mặc định
        videoData.put("hashtags", hashtags);

        db.collection("videos").document(Id).set(videoData);

        String thumbUrl = "https://picsum.photos/200/300";
        if (videoUrl.contains("cloudinary.com")) {
            thumbUrl = videoUrl.replace(".mp4", ".jpg");
            if (thumbUrl.contains("/upload/")) {
                thumbUrl = thumbUrl.replace("/upload/", "/upload/so_0/");
            }
        }

        Map<String, Object> summaryData = new HashMap<>();
        summaryData.put("videoId", Id);
        summaryData.put("thumbnailUri", thumbUrl);
        summaryData.put("watchCount", 0);

        db.collection("video_summaries").document(Id).set(summaryData);
        db.collection("profiles").document(uid).collection("public_videos").document(Id).set(summaryData);

        for (String tag : hashtags) {
            Map<String, Object> h = new HashMap<>();
            h.put("hashtag", tag); 
            h.put("videoId", Id); 
            h.put("thumbnailUri", thumbUrl);
            db.collection("hashtags").add(h);
        }
    }
}
