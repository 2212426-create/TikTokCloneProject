package com.example.tiktokcloneproject.activity;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.viewpager2.widget.ViewPager2;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.adapters.VideoAdapter;
import com.example.tiktokcloneproject.model.Video;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;

public class VideoHomeScreenActivity extends Activity implements View.OnClickListener {
    private FirebaseFirestore db;
    private ViewPager2 viewPager2;
    private ArrayList<Video> videos;
    private VideoAdapter videoAdapter;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private long pressedTime;
    private Button btnHome, btnAddVideo, btnInbox, btnProfile, btnSearch;
    private Intent intentMain = null;
    private ListenerRegistration videoListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_home_screen);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        
        btnHome = findViewById(R.id.btnHome);
        btnAddVideo = findViewById(R.id.btnAddVideo);
        btnInbox = findViewById(R.id.btnInbox);
        btnProfile = findViewById(R.id.btnProfile);
        btnSearch = findViewById(R.id.btnSearch);

        btnHome.setOnClickListener(this);
        btnAddVideo.setOnClickListener(this);
        btnInbox.setOnClickListener(this);
        btnProfile.setOnClickListener(this);
        btnSearch.setOnClickListener(this);

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

        loadVideos();
        intentMain = new Intent(this, HomeScreenActivity.class);
    }

    private void loadVideos() {
        if (db == null) return;

        videoListener = db.collection("videos")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen error", e);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            Video video = dc.getDocument().toObject(Video.class);
                            int newIndex = dc.getNewIndex();
                            int oldIndex = dc.getOldIndex();

                            switch (dc.getType()) {
                                case ADDED:
                                    videos.add(newIndex, video);
                                    videoAdapter.notifyItemInserted(newIndex);
                                    break;
                                case MODIFIED:
                                    if (oldIndex == newIndex) {
                                        videos.set(newIndex, video);
                                        videoAdapter.notifyItemChanged(newIndex);
                                    } else {
                                        videos.remove(oldIndex);
                                        videos.add(newIndex, video);
                                        videoAdapter.notifyItemMoved(oldIndex, newIndex);
                                        videoAdapter.notifyItemChanged(newIndex);
                                    }
                                    break;
                                case REMOVED:
                                    videos.remove(oldIndex);
                                    videoAdapter.notifyItemRemoved(oldIndex);
                                    break;
                            }
                        }
                    }
                });
    }

    @Override
    public void onBackPressed() {
        if (pressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
            finish();
        } else {
            Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT).show();
        }
        pressedTime = System.currentTimeMillis();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btnSearch) {
            intentMain.putExtra("fragment_search", "");
            startActivity(intentMain);
        } else if (id == R.id.btnProfile) {
            handleProfileClick();
        } else if (id == R.id.btnAddVideo) {
            handleAddClick();
        } else if (id == R.id.btnInbox) {
            handleInboxClick();
        }
    }

    private void handleProfileClick() {
        if (user == null) {
            startActivity(new Intent(this, SignupChoiceActivity.class));
            return;
        }
        intentMain.putExtra("fragment_profile", "");
        startActivity(intentMain);
    }

    private void handleAddClick() {
        if (user == null) {
            showNiceDialogBox(this, null, null);
            return;
        }
        startActivity(new Intent(this, CameraActivity.class));
    }

    private void handleInboxClick() {
        if (user == null) {
            showNiceDialogBox(this, null, null);
            return;
        }
        intentMain.putExtra("fragment_inbox", "");
        startActivity(intentMain);
    }

    private void showNiceDialogBox(Context context, @Nullable String title, @Nullable String message) {
        if (title == null) title = getString(R.string.request_account_title);
        if (message == null) message = getString(R.string.request_account_message);
        try {
            new AlertDialog.Builder(context, R.style.AlertDialogTheme)
                    .setIcon(R.drawable.splash_background)
                    .setTitle(title)
                    .setMessage(message)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Sign up/Sign in", (dialog, whichOne) -> {
                        Intent intent = new Intent(context, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    })
                    .show();
        } catch (Exception e) {
            Log.e("Error DialogBox", e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoAdapter != null) {
            videoAdapter.pauseVideo(videoAdapter.getCurrentPosition());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoAdapter != null) {
            videoAdapter.playVideo(videoAdapter.getCurrentPosition());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoListener != null) {
            videoListener.remove();
        }
    }
}
