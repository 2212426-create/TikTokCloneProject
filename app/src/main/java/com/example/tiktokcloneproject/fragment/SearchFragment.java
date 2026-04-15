package com.example.tiktokcloneproject.fragment;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class SearchFragment extends Fragment implements View.OnClickListener {
    private Context context = null;
    private final String TAG = "SearchFragment";
    
    RecyclerView rcv_users;
    UserAdapter userAdapter;
    SearchView searchView;

    ArrayList<VideoSummary> videoSummaries = new ArrayList<>();
    VideoSummaryAdapter videoSummaryAdapter;
    RecyclerView rcvVideoSummary;
    TextView tvSubmitSearch;

    ArrayList<User> userArrayList = new ArrayList<>();
    FirebaseFirestore db;

    public static SearchFragment newInstance(String strArg) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString("name", strArg);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = requireActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.activity_searching, container, false);
        db = FirebaseFirestore.getInstance();

        tvSubmitSearch = layout.findViewById(R.id.tvSubmitSearch);
        searchView = layout.findViewById(R.id.searchView);
        rcv_users = layout.findViewById(R.id.rcv_users);
        rcvVideoSummary = layout.findViewById(R.id.rcvVideoSummary);

        if (searchView != null) {
            searchView.setIconified(false);
            EditText txtSearch = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
            if (txtSearch != null) {
                txtSearch.setTextColor(ContextCompat.getColor(context, android.R.color.white));
                txtSearch.setHintTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
            }

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    performSearch(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (newText == null || newText.trim().isEmpty()) {
                        clearData();
                    } else {
                        performSearch(newText.trim());
                    }
                    return true;
                }
            });
        }

        // Setup User List
        rcv_users.setLayoutManager(new WrapContentLinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        userAdapter = new UserAdapter(context, userArrayList);
        rcv_users.setAdapter(userAdapter);

        // Setup Video Grid
        videoSummaryAdapter = new VideoSummaryAdapter(context, videoSummaries);
        rcvVideoSummary.setLayoutManager(new GridLayoutManager(context, 2)); // 2 cột cho đẹp
        rcvVideoSummary.addItemDecoration(new GridSpacingItemDecoration(2, 15, true));
        rcvVideoSummary.setAdapter(videoSummaryAdapter);

        if (tvSubmitSearch != null) tvSubmitSearch.setOnClickListener(this);

        return layout;
    }

    private void performSearch(String query) {
        if (query.isEmpty()) return;

        // 1. Tìm User
        searchUsers(query);
        
        // 2. Tìm Video theo tiêu đề (description)
        searchVideos(query);
        
        // Hiển thị cả hai hoặc ưu tiên
        rcv_users.setVisibility(View.VISIBLE);
        rcvVideoSummary.setVisibility(View.VISIBLE);
    }

    private void clearData() {
        userArrayList.clear();
        userAdapter.notifyDataSetChanged();
        videoSummaries.clear();
        videoSummaryAdapter.notifyDataSetChanged();
    }

    private void searchUsers(String key) {
        db.collection("profiles")
                .orderBy("username").startAt(key).endAt(key + "\uf8ff")
                .limit(5)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        userArrayList.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            userArrayList.add(new User(doc.getId(), doc.getString("username")));
                        }
                        userAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void searchVideos(String key) {
        db.collection("videos")
                .orderBy("description").startAt(key).endAt(key + "\uf8ff")
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        videoSummaries.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            videoSummaries.add(new VideoSummary(
                                    doc.getId(),
                                    doc.getString("videoUri"), // Hoặc thumbnailUri nếu có
                                    doc.getLong("watchCount")
                            ));
                        }
                        videoSummaryAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.tvSubmitSearch) {
            String query = searchView.getQuery().toString();
            performSearch(query);
            searchView.clearFocus();
        }
    }

    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spanCount, spacing;
        private boolean includeEdge;
        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount; this.spacing = spacing; this.includeEdge = includeEdge;
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
