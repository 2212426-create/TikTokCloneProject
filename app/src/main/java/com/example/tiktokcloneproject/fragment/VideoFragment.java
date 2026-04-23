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
        progressBar = layout.findViewById(R.id.loadingGif);
        
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        videos = new ArrayList<>();
        videoAdapter = new VideoAdapter(context != null ? context : getActivity(), videos);
        VideoAdapter.setUser(user);
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
        return layout;
    }

    public void pauseVideo() {
        if (videoAdapter != null) {
            videoAdapter.pauseVideo(videoAdapter.getCurrentPosition());
        }
    }

    public void continueVideo() {
        if (videoAdapter != null) {
            videoAdapter.playVideo(videoAdapter.getCurrentPosition());
        }
    }

    private void loadVideos() {
        if (db == null) return;
        
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
                        
                        boolean isFirstLoad = videos.isEmpty();
                        
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Video video = dc.getDocument().toObject(Video.class);
                                videos.add(video);
                                videoAdapter.notifyItemInserted(videos.size() - 1);
                            }
                        }
                        
                        // Nếu là lần đầu load, tự động play video đầu tiên sau khi adapter cập nhật
                        if (isFirstLoad && !videos.isEmpty()) {
                            viewPager2.post(() -> {
                                if (videoAdapter != null) videoAdapter.playVideo(0);
                            });
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
    public void onDestroyView() {
        super.onDestroyView();
        if (videoListener != null) {
            videoListener.remove();
            videoListener = null;
        }
    }
}
