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
import android.graphics.drawable.BitmapDrawable;
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
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.security.MessageDigest;
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
    TextView tvTitleScreen;

    final String REGEX_HASHTAG = "#([A-Za-z0-9_\\u00C0-\\u1EF9-]+)";
    String username = "user";
    Uri videoUri;
    final long maximumDuration = 300000;
    final long maximumVideoSize = 100 * 1024 * 1024; // 100MB

    FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseFirestore db;

    ArrayList<String> hashtags;
    String Id;
    final String TAG = "DescriptionVideoActivity";
    Bitmap thumbnail;

    boolean isEditMode = false;
    String targetVideoId;
    String currentThumbUrl;

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
        tvTitleScreen = findViewById(R.id.tvTitleScreen);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        hashtags = new ArrayList<>();

        Intent intent = getIntent();
        if (intent != null) {
            isEditMode = intent.getBooleanExtra("isEditMode", false);
            if (isEditMode) {
                targetVideoId = intent.getStringExtra("videoId");
                setupEditMode();
            } else {
                if (intent.getData() != null) {
                    videoUri = intent.getData();
                } else if (intent.getExtras() != null) {
                    String videoPath = intent.getExtras().getString("videoUri");
                    if (videoPath != null) videoUri = Uri.parse(videoPath);
                }
                if (videoUri != null) processVideoMetadata();
            }
        }
        
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

    private void setupEditMode() {
        if (tvTitleScreen != null) tvTitleScreen.setText("Sửa mô tả video");
        btnDescription.setText("CẬP NHẬT");
        
        db.collection("videos").document(targetVideoId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String desc = doc.getString("description");
                    edtDescription.setText(desc);
                    String videoUrl = doc.getString("videoUri");
                    
                    if (videoUrl != null && !videoUrl.isEmpty()) {
                        currentThumbUrl = videoUrl.replace(".mp4", ".jpg");
                        if (currentThumbUrl.contains("/upload/")) {
                            currentThumbUrl = currentThumbUrl.replace("/upload/", "/upload/so_0/");
                        }
                        Glide.with(this).load(currentThumbUrl).into(imvShortCutVideo);
                    }
                }
            });
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

    private long getVideoSize(Uri uri) {
        if (uri == null) return 0;
        if ("file".equals(uri.getScheme())) {
            String path = uri.getPath();
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    return file.length();
                }
            }
        }
        
        try (android.content.res.AssetFileDescriptor fd = getContentResolver().openAssetFileDescriptor(uri, "r")) {
            if (fd != null) {
                long len = fd.getLength();
                if (len > 0) return len;
            }
        } catch (Exception ignored) {}

        android.database.Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, new String[]{android.provider.OpenableColumns.SIZE}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                if (sizeIndex != -1) {
                    return cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying size: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return 0;
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
                long videoSize = getVideoSize(videoUri);
                
                boolean isDurationInvalid = duration > maximumDuration;
                boolean isSizeInvalid = videoSize > maximumVideoSize;

                if (isDurationInvalid || isSizeInvalid) {
                    runOnUiThread(() -> {
                        if (isDurationInvalid && isSizeInvalid) {
                            Toast.makeText(this, "Video quá dài (Tối đa 5 phút) và vượt quá 100MB!", Toast.LENGTH_LONG).show();
                        } else if (isDurationInvalid) {
                            Toast.makeText(this, "Video quá dài! Tối đa 5 phút.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Dung lượng video vượt quá 100MB!", Toast.LENGTH_LONG).show();
                        }
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
            if (isEditMode) handleUpdateVideo();
            else handlePostVideo();
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
        if (cursorPosition > 0 && currentText.charAt(cursorPosition - 1) != ' ') hashtag = " #";
        edtDescription.getText().insert(cursorPosition, hashtag);
        edtDescription.requestFocus();
    }

    private void handleAiSuggestion() {
        Bitmap bitmapToAnalyze = thumbnail;
        if (isEditMode && bitmapToAnalyze == null) {
            if (imvShortCutVideo.getDrawable() instanceof BitmapDrawable) {
                bitmapToAnalyze = ((BitmapDrawable) imvShortCutVideo.getDrawable()).getBitmap();
            }
        }

        if (bitmapToAnalyze == null) {
            Toast.makeText(this, "Vui lòng đợi video tải xong", Toast.LENGTH_SHORT).show();
            return;
        }

        runOnUiThread(() -> {
            btnAiSuggest.setVisibility(View.GONE);
            pbAiLoading.setVisibility(View.VISIBLE);
        });

        ListenableFuture<GenerateContentResponse> future = GeminiHelper.suggestHashtags(bitmapToAnalyze);
        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> {
                    String suggestion = result.getText();
                    if (suggestion != null) {
                        String currentText = edtDescription.getText().toString();
                        if (!currentText.isEmpty() && !currentText.endsWith(" ")) currentText += " ";
                        edtDescription.setText(currentText + suggestion);
                        edtDescription.setSelection(edtDescription.getText().length());
                    }
                    pbAiLoading.setVisibility(View.GONE);
                    btnAiSuggest.setVisibility(View.VISIBLE);
                });
            }
            @Override public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    pbAiLoading.setVisibility(View.GONE);
                    btnAiSuggest.setVisibility(View.VISIBLE);
                    Toast.makeText(DescriptionVideoActivity.this, "AI tạm thời không khả dụng", Toast.LENGTH_SHORT).show();
                });
            }
        }, Executors.newSingleThreadExecutor());
    }

    private void handleUpdateVideo() {
        final String newDescription = edtDescription.getText().toString().trim();
        hashtags.clear();
        Matcher matcher = Pattern.compile(REGEX_HASHTAG).matcher(newDescription);
        while (matcher.find()) hashtags.add(matcher.group(1).toLowerCase());

        Map<String, Object> updates = new HashMap<>();
        updates.put("description", newDescription);
        updates.put("hashtags", hashtags);

        db.collection("videos").document(targetVideoId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                // Đồng bộ sang các collection mirror để đảm bảo grid view cũng cập nhật
                db.collection("video_summaries").document(targetVideoId).set(updates, SetOptions.merge());
                if (user != null) {
                    db.collection("profiles").document(user.getUid())
                      .collection("public_videos").document(targetVideoId)
                      .set(updates, SetOptions.merge());
                }
                
                updateHashtagsCollection();
                Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateHashtagsCollection() {
        db.collection("hashtags").whereEqualTo("videoId", targetVideoId).get()
            .addOnSuccessListener(snapshots -> {
                WriteBatch batch = db.batch();
                for (DocumentSnapshot doc : snapshots) batch.delete(doc.getReference());
                
                for (String tag : hashtags) {
                    Map<String, Object> h = new HashMap<>();
                    h.put("hashtag", tag);
                    h.put("videoId", targetVideoId);
                    h.put("thumbnailUri", currentThumbUrl);
                    batch.set(db.collection("hashtags").document(), h);
                }
                batch.commit();
            });
    }

    private void handlePostVideo() {
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập trước", Toast.LENGTH_SHORT).show();
            return;
        }

        long videoSize = getVideoSize(videoUri);
        if (videoSize > maximumVideoSize) {
            Toast.makeText(this, "Dung lượng video vượt quá 100MB!", Toast.LENGTH_LONG).show();
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

                if (is == null) throw new IOException("Cannot open input stream");

                try (OutputStream os = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = is.read(buffer)) > 0) os.write(buffer, 0, length);
                } finally {
                    try { is.close(); } catch (Exception ignored) {}
                }

                if (tempFile.length() > maximumVideoSize) {
                    tempFile.delete();
                    runOnUiThread(() -> {
                        Toast.makeText(appCtx, "Dung lượng video vượt quá 100MB!", Toast.LENGTH_LONG).show();
                        btnDescription.setEnabled(true);
                    });
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
                                verifyCopyrightAndSave(videoUrl, description, currentUid, finalUsername, tempFile);
                                mBuilder.setContentTitle("Đang kiểm tra bản quyền").setContentText("Video đang được kiểm tra trước khi duyệt...").setProgress(0, 0, false).setOngoing(false);
                                safeNotify();
                            }

                            @Override
                            public void onError(String requestId, ErrorInfo error) {
                                tempFile.delete();
                                mBuilder.setContentTitle("Tải lên thất bại").setContentText(error != null ? error.getDescription() : "Lỗi").setProgress(0, 0, false).setOngoing(false);
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

    private String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) result.append(String.format("%02x", value));
        return result.toString();
    }

    private void verifyCopyrightAndSave(String videoUrl, String description, String uid, String uName, File tempFile) {
        final String fileHash;
        try {
            fileHash = sha256(tempFile);
        } catch (Exception e) {
            Log.e(TAG, "Fingerprint error", e);
            tempFile.delete();
            runOnUiThread(() -> Toast.makeText(this, "Không thể kiểm tra bản quyền video", Toast.LENGTH_LONG).show());
            return;
        }

        db.collection("videos").whereEqualTo("copyrightFileHash", fileHash).limit(1).get()
                .addOnSuccessListener(matches -> {
                    if (!matches.isEmpty()) {
                        tempFile.delete();
                        runOnUiThread(() -> Toast.makeText(this, "Video đã tồn tại trên TopTop và bị từ chối", Toast.LENGTH_LONG).show());
                        return;
                    }
                    saveDataToFirestore(videoUrl, description, uid, uName, fileHash);
                    tempFile.delete();
                    mBuilder.setContentTitle("Tải lên thành công").setContentText("Video đang chờ kiểm duyệt.").setProgress(0, 0, false).setOngoing(false);
                    safeNotify();
                })
                .addOnFailureListener(error -> {
                    tempFile.delete();
                    runOnUiThread(() -> Toast.makeText(this, "Không thể kiểm tra bản quyền: " + error.getMessage(), Toast.LENGTH_LONG).show());
                });
    }

    private void saveDataToFirestore(String videoUrl, String description, String uid, String uName, String fileHash) {
        if (uid.isEmpty()) return;
        Map<String, Object> videoData = new HashMap<>();
        videoData.put("videoId", Id);
        videoData.put("videoUri", videoUrl != null ? videoUrl : "");
        videoData.put("authorId", uid);
        videoData.put("username", uName);
        videoData.put("description", description);
        videoData.put("totalLikes", 0);
        videoData.put("totalComments", 0);
        videoData.put("watchCount", 0);
        videoData.put("timestamp", System.currentTimeMillis());
        videoData.put("moderationStatus", "pending");
        videoData.put("copyrightFileHash", fileHash);
        videoData.put("hashtags", hashtags);

        db.collection("videos").document(Id).set(videoData);

        String thumbUrl = "https://picsum.photos/200/300";
        if (videoUrl != null && videoUrl.contains("cloudinary.com")) {
            thumbUrl = videoUrl.replace(".mp4", ".jpg");
            if (thumbUrl.contains("/upload/")) thumbUrl = thumbUrl.replace("/upload/", "/upload/so_0/");
        }

        Map<String, Object> summaryData = new HashMap<>();
        summaryData.put("videoId", Id);
        summaryData.put("thumbnailUri", thumbUrl);
        summaryData.put("watchCount", 0);

        db.collection("video_summaries").document(Id).set(summaryData);
        db.collection("profiles").document(uid).collection("public_videos").document(Id).set(summaryData);

        for (String tag : hashtags) {
            Map<String, Object> h = new HashMap<>();
            h.put("hashtag", tag); h.put("videoId", Id); h.put("thumbnailUri", thumbUrl);
            db.collection("hashtags").add(h);
        }
    }
}
