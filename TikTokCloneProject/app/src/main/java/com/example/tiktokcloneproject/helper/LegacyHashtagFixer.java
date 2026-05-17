package com.example.tiktokcloneproject.helper;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.widget.Toast;

import com.example.tiktokcloneproject.model.Video;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LegacyHashtagFixer {
    private static final String TAG = "LegacyHashtagFixer";
    private static final String REGEX_HASHTAG = "#([A-Za-z0-9_\\u00C0-\\u1EF9-]+)";
    
    private static final ExecutorService aiExecutor = Executors.newFixedThreadPool(3);
    private static final Set<String> processingVideos = new HashSet<>();

    public static void fixMissingHashtags(Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("videos").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Video video = document.toObject(Video.class);
                fixSingleVideo(context, video);
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error fetching videos for hashtag fix: " + e.getMessage()));
    }

    public static void fixSingleVideo(Context context, Video video) {
        if (video == null || video.getVideoId() == null || video.getVideoUri() == null) return;
        
        if (!isMissingHashtags(video) || processingVideos.contains(video.getVideoId())) {
            return;
        }

        processingVideos.add(video.getVideoId());
        
        // Thông báo cho người dùng biết AI đã bắt đầu
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> 
                Toast.makeText(context, "AI đang 'xem' video để tìm hashtag viral...", Toast.LENGTH_SHORT).show());
        }

        processVideo(context, video);
    }

    private static boolean isMissingHashtags(Video video) {
        List<String> tags = video.getHashtags();
        String desc = video.getDescription();
        return (tags == null || tags.isEmpty()) && (desc == null || !desc.contains("#"));
    }

    private static void processVideo(Context context, Video video) {
        aiExecutor.execute(() -> {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                // Cloudinary URL works with MediaMetadataRetriever
                retriever.setDataSource(video.getVideoUri(), new HashMap<>());
                Bitmap bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC); 
                
                if (bitmap != null) {
                    ListenableFuture<GenerateContentResponse> future = GeminiHelper.suggestHashtags(bitmap);
                    Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
                        @Override
                        public void onSuccess(GenerateContentResponse result) {
                            String hashtagString = result.getText();
                            if (hashtagString != null && !hashtagString.isEmpty()) {
                                List<String> tagList = new ArrayList<>();
                                Matcher matcher = Pattern.compile(REGEX_HASHTAG).matcher(hashtagString);
                                while (matcher.find()) {
                                    tagList.add(matcher.group(1).toLowerCase());
                                }
                                updateVideo(context, video, hashtagString, tagList);
                            }
                            processingVideos.remove(video.getVideoId());
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            Log.e(TAG, "AI Failed: " + t.getMessage());
                            processingVideos.remove(video.getVideoId());
                        }
                    }, aiExecutor);
                } else {
                    processingVideos.remove(video.getVideoId());
                }
            } catch (Exception e) {
                Log.e(TAG, "Retriever error: " + e.getMessage());
                processingVideos.remove(video.getVideoId());
            } finally {
                try { retriever.release(); } catch (Exception ignored) {}
            }
        });
    }

    private static void updateVideo(Context context, Video video, String hashtagString, List<String> tagList) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentDesc = video.getDescription() != null ? video.getDescription() : "";
        String updatedDesc = currentDesc + " " + hashtagString;
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", updatedDesc.trim());
        updates.put("hashtags", tagList);

        db.collection("videos").document(video.getVideoId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "AI Fix Success: " + video.getVideoId());
                    if (context instanceof Activity) {
                        ((Activity) context).runOnUiThread(() -> 
                            Toast.makeText(context, "AI đã tìm thấy hashtag cho video!", Toast.LENGTH_SHORT).show());
                    }
                });
    }
}
