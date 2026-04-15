package com.example.tiktokcloneproject.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

public class VideoFragment extends Fragment {
    private Context context = null;
    private ViewPager2 viewPager2;
    private ArrayList<Video> videos;
    private VideoAdapter videoAdapter;
    private ProgressBar progressBar;
    
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private ListenerRegistration videoListener;
    private final String TAG = "VideoFragment";

    public static VideoFragment newInstance(String strArg) {
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        args.putString("name", strArg);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_video, container, false);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        viewPager2 = layout.findViewById(R.id.viewPager);
        progressBar = layout.findViewById(R.id.loadingGif); // Sử dụng ID cũ cho ProgressBar
        
        // Nếu không tìm thấy ProgressBar theo ID cũ, tạo mới hoặc ẩn đi
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        videos = new ArrayList<>();
        videoAdapter = new VideoAdapter(context != null ? context : getActivity(), videos);
        VideoAdapter.setUser(user);
        viewPager2.setAdapter(videoAdapter);
        
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (videoAdapter != null && position < videos.size()) {
                    videoAdapter.pauseVideo(videoAdapter.getCurrentPosition());
                    videoAdapter.playVideo(position);
                    videoAdapter.updateWatchCount(position);
                    videoAdapter.updateCurrentPosition(position);
                }
            }
        });

        loadVideos();
        return layout;
    }

    public void pauseVideo() {
        if (videoAdapter != null) {
            int currentPosition = videoAdapter.getCurrentPosition();
            Context ctx = context != null ? context : getActivity();
            if (ctx != null) {
                SharedPreferences currentPosPref = ctx.getSharedPreferences("position", Context.MODE_PRIVATE);
                currentPosPref.edit().putInt("position", currentPosition).apply();
            }
            videoAdapter.pauseVideo(currentPosition);
        }
    }

    public void continueVideo() {
        if (videoAdapter != null) {
            Context ctx = context != null ? context : getActivity();
            if (ctx != null) {
                SharedPreferences currentPosPref = ctx.getSharedPreferences("position", Context.MODE_PRIVATE);
                int currentPosition = currentPosPref.getInt("position", -1);
                if (currentPosition != -1 && currentPosition < videos.size()) {
                    videoAdapter.playVideo(currentPosition);
                }
            }
        }
    }

    private void loadVideos() {
        if (db == null) return;
        
        // Lấy 10 video mới nhất để giảm lag khi khởi động
        videoListener = db.collection("videos")
                .limit(10)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Lỗi tải video: " + e.getMessage());
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        return;
                    }
                    
                    if (snapshots != null) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Video video = dc.getDocument().toObject(Video.class);
                                if (videoAdapter != null) {
                                    videos.add(video);
                                    videoAdapter.notifyItemInserted(videos.size() - 1);
                                }
                            }
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (videoListener != null) {
            videoListener.remove();
            videoListener = null;
        }
    }
}
