package com.example.tiktokcloneproject.activity;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.adapters.VideoAdapter;
import com.example.tiktokcloneproject.model.Video;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class VideoActivity extends Activity {
    private String videoId;
    private FirebaseFirestore db;
    private ViewPager2 viewPager2;
    private ArrayList<Video> videos;
    private VideoAdapter videoAdapter;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private ListenerRegistration videoListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (intent.hasExtra("videoId")) {
            videoId = bundle.getString("videoId");
        } else if (intent.getData() != null) {
            Uri data = intent.getData();
            List<String> segmentsList = data.getPathSegments();
            videoId = segmentsList.get(segmentsList.size() - 1);
        }

        if (videoId == null) {
            finish();
            return;
        }

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        viewPager2 = findViewById(R.id.viewPager);
        videos = new ArrayList<>();
        videoAdapter = new VideoAdapter(this, videos);
        videoAdapter.setUser(user);
        viewPager2.setAdapter(videoAdapter);
        
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (videoAdapter != null) {
                    videoAdapter.pauseVideo(videoAdapter.getCurrentPosition());
                    videoAdapter.updateCurrentPosition(position);
                    videoAdapter.playVideo(position);
                    videoAdapter.updateWatchCount(position);
                }
            }
        });

        db = FirebaseFirestore.getInstance();
        
        // SỬA LỖI: Chuyển từ get() sang addSnapshotListener để cập nhật mô tả ngay lập tức khi sửa xong
        videoListener = db.collection("videos").document(videoId)
                .addSnapshotListener((document, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed.", error);
                        return;
                    }

                    if (document != null && document.exists()) {
                        Video video = document.toObject(Video.class);
                        if (video != null) {
                            if ("rejected".equals(video.getModerationStatus())) {
                                android.widget.Toast.makeText(VideoActivity.this, "Video này đã bị gỡ bỏ.", android.widget.Toast.LENGTH_LONG).show();
                                finish();
                                return;
                            }
                            
                            if (videos.isEmpty()) {
                                videos.add(video);
                                videoAdapter.notifyItemInserted(0);
                                viewPager2.post(() -> videoAdapter.playVideo(0));
                            } else {
                                videos.set(0, video);
                                // Cập nhật Metadata (mô tả/hashtag) mà không reset video
                                videoAdapter.notifyItemChanged(0, "METADATA_UPDATE");
                            }
                        }
                    } else {
                        Log.d(TAG, "Document does not exist");
                        if (!videos.isEmpty()) {
                            finish(); // Video đã bị xóa
                        }
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        continueVideo();
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseVideo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoListener != null) {
            videoListener.remove();
        }
    }

    public void pauseVideo() {
        if (videoAdapter != null) {
            int currentPosition = videoAdapter.getCurrentPosition();
            SharedPreferences currentPosPref = this.getSharedPreferences("position", Context.MODE_PRIVATE);
            SharedPreferences.Editor positionEditor = currentPosPref.edit();
            positionEditor.putInt("position", currentPosition);
            positionEditor.apply();
            videoAdapter.pauseVideo(currentPosition);
        }
    }

    public void continueVideo() {
        if (videoAdapter != null) {
            SharedPreferences currentPosPref = this.getSharedPreferences("position", Context.MODE_PRIVATE);
            int currentPosition = currentPosPref.getInt("position", 0);
            videoAdapter.playVideo(currentPosition);
        }
    }

    public void onClick(View v) {
        if (v.getId() == R.id.btnBackVideo) {
            this.finish();
        }
    }
}
