package com.example.tiktokcloneproject.helper;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecommendationHelper {
    private static final String TAG = "RecommendationHelper";
    private static final String REGEX_HASHTAG = "#([A-Za-z0-9_-]+)";

    // Bước 2: Ghi lại sở thích khi xem/like video
    public static void recordInterest(String description) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || description == null || description.isEmpty()) return;

        List<String> hashtags = new ArrayList<>();
        Matcher matcher = Pattern.compile(REGEX_HASHTAG).matcher(description);
        while (matcher.find()) {
            hashtags.add(matcher.group(1).toLowerCase());
        }

        if (hashtags.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for (String tag : hashtags) {
            db.collection("user_interests")
                    .document(user.getUid())
                    .collection("tags")
                    .document(tag)
                    .set(new HashMap<String, Object>() {{
                        put("count", FieldValue.increment(1));
                        put("lastUpdated", System.currentTimeMillis());
                    }}, com.google.firebase.firestore.SetOptions.merge());
        }
    }

    // Bước 3: Lấy các tag mà người dùng quan tâm nhất
    public static Task<List<String>> getTopInterests() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return Tasks.forResult(new ArrayList<>());

        return FirebaseFirestore.getInstance()
                .collection("user_interests")
                .document(user.getUid())
                .collection("tags")
                .orderBy("count", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .continueWith(task -> {
                    List<String> topTags = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            topTags.add(doc.getId());
                        }
                    }
                    return topTags;
                });
    }
}
