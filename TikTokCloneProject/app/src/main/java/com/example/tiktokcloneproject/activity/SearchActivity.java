package com.example.tiktokcloneproject.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.WrapContentLinearLayoutManager;
import com.example.tiktokcloneproject.adapters.UserAdapter;
import com.example.tiktokcloneproject.adapters.VideoSummaryAdapter;
import com.example.tiktokcloneproject.model.User;
import com.example.tiktokcloneproject.model.VideoSummary;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class SearchActivity extends Activity implements View.OnClickListener {

    final String USERNAME_LABEL = "username";
    RecyclerView rcv_users;
    UserAdapter userAdapter;
    SearchView searchView;

    ArrayList<VideoSummary> videoSummaries = new ArrayList<>();
    VideoSummaryAdapter videoSummaryAdapter;
    RecyclerView rcvVideoSummary;

    ImageButton imbBackToHome;
    TextView tvSubmitSearch;

    final String TAG = "SearchActivity";
    ArrayList<User> userArrayList = new ArrayList<>();

    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searching);

        db = FirebaseFirestore.getInstance();

        // KHỞI TẠO CÁC VIEW
        tvSubmitSearch = findViewById(R.id.tvSubmitSearch);
        imbBackToHome = findViewById(R.id.imbBackToHome);
        searchView = findViewById(R.id.searchView);
        rcv_users = findViewById(R.id.rcv_users);
        rcvVideoSummary = findViewById(R.id.rcvVideoSummary);

        // Thiết lập User list
        rcv_users.setLayoutManager(new WrapContentLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        userAdapter = new UserAdapter(this, userArrayList);
        rcv_users.setAdapter(userAdapter);
        rcv_users.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // Thiết lập Video list
        videoSummaryAdapter = new VideoSummaryAdapter(getApplicationContext(), videoSummaries);
        rcvVideoSummary.setLayoutManager(new GridLayoutManager(this, 3));
        rcvVideoSummary.addItemDecoration(new GridSpacingItemDecoration(3, 10, true));
        rcvVideoSummary.setAdapter(videoSummaryAdapter);

        // Đăng ký sự kiện click
        if (imbBackToHome != null) imbBackToHome.setOnClickListener(this);
        if (tvSubmitSearch != null) tvSubmitSearch.setOnClickListener(this);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText != null && !newText.trim().isEmpty()) {
                    String query = newText.trim();
                    if (query.startsWith("#")) {
                        rcv_users.setVisibility(View.GONE);
                        rcvVideoSummary.setVisibility(View.VISIBLE);
                        setVideoSummaries(query);
                    } else {
                        rcv_users.setVisibility(View.VISIBLE);
                        rcvVideoSummary.setVisibility(View.GONE);
                        getData(query);
                    }
                } else {
                    clearData();
                }
                return true;
            }
        });
    }

    private void clearData() {
        runOnUiThread(() -> {
            userArrayList.clear();
            userAdapter.notifyDataSetChanged();
            videoSummaries.clear();
            videoSummaryAdapter.notifyDataSetChanged();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userAdapter != null) {
            userAdapter.release();
        }
    }

    private void getData(String key) {
        db.collection("users")
                .orderBy(USERNAME_LABEL)
                .startAt(key)
                .endAt(key + "\uf8ff")
                .limit(20)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        ArrayList<User> newList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            newList.add(new User(document.getString("userId"), document.getString(USERNAME_LABEL)));
                        }
                        updateUserList(newList);
                    }
                });
    }

    private void updateUserList(ArrayList<User> newList) {
        runOnUiThread(() -> {
            userArrayList.clear();
            userArrayList.addAll(newList);
            userAdapter.notifyDataSetChanged();
        });
    }

    private void setVideoSummaries(String hashtag) {
        db.collection("hashtags").document(hashtag).collection("video_summaries")
                .limit(20)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        ArrayList<VideoSummary> newList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            newList.add(document.toObject(VideoSummary.class));
                        }
                        updateVideoList(newList);
                    }
                });
    }

    private void updateVideoList(ArrayList<VideoSummary> newList) {
        runOnUiThread(() -> {
            videoSummaries.clear();
            videoSummaries.addAll(newList);
            videoSummaryAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onClick(View view) {
        if (imbBackToHome != null && view.getId() == imbBackToHome.getId()) {
            finish();
        }
        if (tvSubmitSearch != null && view.getId() == tvSubmitSearch.getId()) {
            searchView.clearFocus();
        }
    }

    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;
                if (position < spanCount) outRect.top = spacing;
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) outRect.top = spacing;
            }
        }
    }
}
