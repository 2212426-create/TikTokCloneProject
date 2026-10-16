package com.example.tiktokcloneproject.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.WrapContentLinearLayoutManager;
import com.example.tiktokcloneproject.adapters.TrendingTagAdapter;
import com.example.tiktokcloneproject.adapters.UserAdapter;
import com.example.tiktokcloneproject.adapters.VideoSummaryAdapter;
import com.example.tiktokcloneproject.model.User;
import com.example.tiktokcloneproject.model.VideoSummary;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchFragment extends Fragment implements View.OnClickListener, TrendingTagAdapter.OnTagClickListener {
    private Context context = null;
    private final String TAG = "SearchFragment";
    
    RecyclerView rcv_users;
    UserAdapter userAdapter;
    SearchView searchView;

    ArrayList<VideoSummary> videoSummaries = new ArrayList<>();
    VideoSummaryAdapter videoSummaryAdapter;
    RecyclerView rcvVideoSummary;
    TextView tvSubmitSearch;
    ImageButton imbBackToHome;

    // Trending Tags
    RecyclerView rcvTrendingTags;
    TrendingTagAdapter trendingTagAdapter;
    ArrayList<String> trendingTagsList = new ArrayList<>();
    LinearLayout llTrendingTags;

    // Search History
    RecyclerView rcvSearchHistory;
    TrendingTagAdapter historyAdapter;
    ArrayList<String> historyList = new ArrayList<>();
    LinearLayout llSearchHistory;
    TextView tvClearHistory;
    private static final String PREFS_NAME = "search_prefs";
    private static final String HISTORY_KEY = "search_history";

    ArrayList<User> userArrayList = new ArrayList<>();
    FirebaseFirestore db;

    public static SearchFragment newInstance(String strArg) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString("name", strArg);
        fragment.setArguments(args);
        return fragment;
    }

    public static SearchFragment newInstance(String strArg, String query) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString("name", strArg);
        if (query != null) args.putString("query", query);
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
        imbBackToHome = layout.findViewById(R.id.imbBackToHome);
        searchView = layout.findViewById(R.id.searchView);
        rcv_users = layout.findViewById(R.id.rcv_users);
        rcvVideoSummary = layout.findViewById(R.id.rcvVideoSummary);
        
        // Cập nhật màu cho nút Back (curved arrow)
        if (imbBackToHome != null) {
            imbBackToHome.setColorFilter(ContextCompat.getColor(context, R.color.tiktok_red));
        }

        // Trending Tags UI
        rcvTrendingTags = layout.findViewById(R.id.rcvTrendingTags);
        llTrendingTags = layout.findViewById(R.id.llTrendingTags);

        // Search History UI
        rcvSearchHistory = layout.findViewById(R.id.rcvSearchHistory);
        llSearchHistory = layout.findViewById(R.id.llSearchHistory);
        tvClearHistory = layout.findViewById(R.id.tvClearHistory);

        if (searchView != null) {
            searchView.setIconified(false);
            EditText txtSearch = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
            if (txtSearch != null) {
                txtSearch.setTextColor(ContextCompat.getColor(context, android.R.color.white));
                txtSearch.setHintTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
            }

            // Cập nhật màu cho các icon trong SearchView (Kính lúp và nút Xóa/Close)
            ImageView searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
            if (searchIcon != null) {
                searchIcon.setColorFilter(ContextCompat.getColor(context, R.color.tiktok_red));
            }
            ImageView closeIcon = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
            if (closeIcon != null) {
                closeIcon.setColorFilter(ContextCompat.getColor(context, R.color.tiktok_red));
            }

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    performSearch(query);
                    saveSearchToHistory(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (newText == null || newText.trim().isEmpty()) {
                        loadTrendingVideos();
                        showDiscoveryUI(true);
                    } else {
                        performSearch(newText.trim());
                        showDiscoveryUI(false);
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
        rcvVideoSummary.setLayoutManager(new GridLayoutManager(context, 2)); 
        rcvVideoSummary.addItemDecoration(new GridSpacingItemDecoration(2, 15, true));
        rcvVideoSummary.setAdapter(videoSummaryAdapter);

        // Setup Trending Tags
        trendingTagAdapter = new TrendingTagAdapter(trendingTagsList, this);
        rcvTrendingTags.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        rcvTrendingTags.setAdapter(trendingTagAdapter);

        // Setup History
        historyAdapter = new TrendingTagAdapter(historyList, this);
        rcvSearchHistory.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        rcvSearchHistory.setAdapter(historyAdapter);

        if (tvSubmitSearch != null) tvSubmitSearch.setOnClickListener(this);
        if (imbBackToHome != null) imbBackToHome.setOnClickListener(this);
        if (tvClearHistory != null) tvClearHistory.setOnClickListener(v -> clearSearchHistory());

        // Initial Data Load
        loadTrendingVideos();
        loadTrendingTags();
        loadSearchHistory();

        // Handle initial query
        if (getArguments() != null && getArguments().containsKey("query")) {
            String q = getArguments().getString("query");
            if (q != null && !q.isEmpty()) {
                searchView.post(() -> {
                    onTagClick(q);
                });
            }
        }

        return layout;
    }

    private void showDiscoveryUI(boolean show) {
        if (llTrendingTags != null) llTrendingTags.setVisibility(show ? View.VISIBLE : View.GONE);
        if (llSearchHistory != null) {
            llSearchHistory.setVisibility(show && !historyList.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (rcv_users != null) rcv_users.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void loadSearchHistory() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(HISTORY_KEY, null);
        if (json != null) {
            Gson gson = new Gson();
            ArrayList<String> saved = gson.fromJson(json, new TypeToken<ArrayList<String>>(){}.getType());
            historyList.clear();
            historyList.addAll(saved);
            historyAdapter.notifyDataSetChanged();
        }
        if (llSearchHistory != null) llSearchHistory.setVisibility(historyList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void saveSearchToHistory(String query) {
        if (query == null || query.trim().isEmpty()) return;
        query = query.trim();
        
        historyList.remove(query);
        historyList.add(0, query);
        if (historyList.size() > 10) historyList.remove(10);
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(HISTORY_KEY, new Gson().toJson(historyList)).apply();
        historyAdapter.notifyDataSetChanged();
        if (llSearchHistory != null) llSearchHistory.setVisibility(View.VISIBLE);
    }

    private void clearSearchHistory() {
        historyList.clear();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(HISTORY_KEY).apply();
        historyAdapter.notifyDataSetChanged();
        if (llSearchHistory != null) llSearchHistory.setVisibility(View.GONE);
    }

    private void loadTrendingTags() {
        db.collection("videos")
                .orderBy("watchCount", Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> uniqueTags = new HashSet<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        List<String> hashtags = (List<String>) doc.get("hashtags");
                        if (hashtags != null) {
                            for (String tag : hashtags) {
                                if (uniqueTags.size() < 10) uniqueTags.add("#" + tag);
                            }
                        }
                    }
                    if (uniqueTags.isEmpty()) {
                        uniqueTags.add("#trending"); uniqueTags.add("#fyp"); uniqueTags.add("#tiktok");
                    }
                    trendingTagsList.clear();
                    trendingTagsList.addAll(uniqueTags);
                    trendingTagAdapter.notifyDataSetChanged();
                });
    }

    private void loadTrendingVideos() {
        userArrayList.clear();
        userAdapter.notifyDataSetChanged();
        
        db.collection("videos")
                .orderBy("watchCount", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        videoSummaries.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            if ("rejected".equals(doc.getString("moderationStatus")) || "pending".equals(doc.getString("moderationStatus"))) continue;
                            String thumb = doc.getString("thumbnailUri");
                            if (thumb == null || thumb.isEmpty()) thumb = doc.getString("videoUri");
                            videoSummaries.add(new VideoSummary(doc.getId(), thumb, doc.getLong("watchCount")));
                        }
                        videoSummaryAdapter.notifyDataSetChanged();
                        rcvVideoSummary.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void performSearch(String query) {
        if (query.isEmpty()) { loadTrendingVideos(); return; }
        searchUsers(query);
        searchVideos(query);
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
                            userArrayList.add(new User(doc.getId(), doc.getString("username"), doc.getString("avatarUrl"), doc.getString("email")));
                        }
                        userAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void searchVideos(String key) {
        Query query;
        if (key.startsWith("#") && !key.contains(" ")) {
            String tag = key.substring(1).toLowerCase();
            query = db.collection("videos").whereArrayContains("hashtags", tag).limit(10);
        } else {
            query = db.collection("videos")
                    .orderBy("description").startAt(key).endAt(key + "\uf8ff")
                    .limit(10);
        }
        
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                videoSummaries.clear();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    if ("rejected".equals(doc.getString("moderationStatus")) || "pending".equals(doc.getString("moderationStatus"))) continue;
                    String thumb = doc.getString("thumbnailUri");
                    if (thumb == null || thumb.isEmpty()) thumb = doc.getString("videoUri");
                    videoSummaries.add(new VideoSummary(doc.getId(), thumb, doc.getLong("watchCount")));
                }
                
                if (videoSummaries.isEmpty() && !key.startsWith("#")) {
                    searchVideosAsHashtag(key);
                } else {
                    videoSummaryAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void searchVideosAsHashtag(String tag) {
        db.collection("videos").whereArrayContains("hashtags", tag.toLowerCase()).limit(10).get()
            .addOnSuccessListener(snapshots -> {
                if (!snapshots.isEmpty()) {
                    videoSummaries.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        if ("rejected".equals(doc.getString("moderationStatus")) || "pending".equals(doc.getString("moderationStatus"))) continue;
                        String thumb = doc.getString("thumbnailUri");
                        if (thumb == null || thumb.isEmpty()) thumb = doc.getString("videoUri");
                        videoSummaries.add(new VideoSummary(doc.getId(), thumb, doc.getLong("watchCount")));
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
            saveSearchToHistory(query);
            searchView.clearFocus();
        } else if (view.getId() == R.id.imbBackToHome) {
            if (getActivity() != null) {
                View btnHome = getActivity().findViewById(R.id.btnHome);
                if (btnHome != null) btnHome.performClick();
            }
        }
    }

    @Override
    public void onTagClick(String tag) {
        if (searchView != null) {
            searchView.setQuery(tag, true);
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
