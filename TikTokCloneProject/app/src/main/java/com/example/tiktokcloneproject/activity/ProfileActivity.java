package com.example.tiktokcloneproject.activity;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.adapters.VideoSummaryAdapter;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.example.tiktokcloneproject.model.Notification;
import com.example.tiktokcloneproject.model.VideoSummary;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends FragmentActivity implements View.OnClickListener {
    final String USERNAME_LABEL = "username";
    private TextView txvFollowing, txvFollowers, txvLikes, txvUserName;
    private EditText edtBio;
    private Button btn, btnEditProfile, btnUpdateBio, btnCancelUpdateBio, btnMessage;
    private LinearLayout llFollowing, llFollowers;
    ImageView imvAvatarProfile;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseUser user;
    String userId;
    DocumentReference docRef;
    String oldBioText, currentUserID;
    RecyclerView recVideoSummary;
    ArrayList<VideoSummary> videoSummaries;
    private int totalLikes = 0;
    private String currentAvatarUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Intent intent = getIntent();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        
        if (intent.getExtras() != null && intent.hasExtra("id")) {
            userId = intent.getStringExtra("id");
        } else if (intent.getData() != null) {
            List<String> segmentsList = intent.getData().getPathSegments();
            userId = segmentsList.get(segmentsList.size() - 1);
        } else if (user != null) {
            userId = user.getUid();
        }
        
        if (userId == null || userId.isEmpty()) {
            finish();
            return;
        }

        txvFollowing = findViewById(R.id.text_following);
        txvFollowers = findViewById(R.id.text_followers);
        txvLikes = findViewById(R.id.text_likes);
        txvUserName = findViewById(R.id.txv_username);
        edtBio = findViewById(R.id.edt_bio);
        btnEditProfile = findViewById(R.id.button_edit_profile);
        btnMessage = findViewById(R.id.button_message);
        imvAvatarProfile = findViewById(R.id.imvAvatarProfile);
        llFollowers = findViewById(R.id.ll_followers);
        llFollowing = findViewById(R.id.ll_following);

        recVideoSummary = findViewById(R.id.recycle_view_video_summary);
        btnUpdateBio = findViewById(R.id.btn_update_bio);
        btnCancelUpdateBio = findViewById(R.id.btn_cancel_update_bio);

        if (btnUpdateBio != null) btnUpdateBio.setOnClickListener(this);
        if (btnCancelUpdateBio != null) btnCancelUpdateBio.setOnClickListener(this);
        llFollowers.setOnClickListener(this);
        llFollowing.setOnClickListener(this);
        if (btnMessage != null) btnMessage.setOnClickListener(this);
        imvAvatarProfile.setOnClickListener(this);

        db = FirebaseFirestore.getInstance();
        setLikes(userId);
        docRef = db.collection("profiles").document(userId);

        if (user != null) {
            currentUserID = user.getUid();
            if (userId.equals(user.getUid())) {
                btnEditProfile.setVisibility(View.VISIBLE);
                edtBio.setVisibility(View.VISIBLE);
                loadUserBio();
                btnEditProfile.setOnClickListener(this);
            } else {
                handleFollow();
            }
        } else {
            handleFollow();
        }

        videoSummaries = new ArrayList<>();
        recVideoSummary.setLayoutManager(new GridLayoutManager(this, 3));
        recVideoSummary.addItemDecoration(new GridSpacingItemDecoration(3, 10, true));
        setVideoSummaries();
    }

    @Override
    public void onStart() {
        super.onStart();
        loadProfileData();
    }

    private void loadProfileData() {
        if (docRef != null) {
            docRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        txvFollowing.setText(String.valueOf(document.get("following") != null ? document.get("following") : 0));
                        txvFollowers.setText(String.valueOf(document.get("followers") != null ? document.get("followers") : 0));
                        txvLikes.setText(String.valueOf(document.get("likes") != null ? document.get("likes") : 0));
                        txvUserName.setText("@" + document.getString(USERNAME_LABEL));
                        
                        currentAvatarUrl = document.getString("avatarUrl");
                        if (currentAvatarUrl != null) {
                            Glide.with(this)
                                    .load(currentAvatarUrl)
                                    .placeholder(R.drawable.default_avatar)
                                    .circleCrop()
                                    .into(imvAvatarProfile);
                        }
                        
                        String bio = document.getString("bio");
                        if (bio != null && edtBio != null) {
                            oldBioText = bio;
                            edtBio.setText(bio);
                        }
                    }
                }
            });
        }
    }

    private void handleFollow() {
        btn = findViewById(R.id.button_follow);
        if (btn == null) return;
        btn.setVisibility(View.VISIBLE);
        if (btnMessage != null) btnMessage.setVisibility(View.VISIBLE);

        if (user != null) {
            db.collection("profiles").document(currentUserID)
                    .collection("following").document(userId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    handleFollowed();
                } else {
                    handleUnfollowed();
                }
            });
        } else {
            btn.setOnClickListener(view -> startActivity(new Intent(ProfileActivity.this, MainActivity.class)));
        }
    }

    protected void setVideoSummaries() {
        if (userId == null) return;
        // BỎ orderBy ĐỂ TRÁNH LỖI THIẾU INDEX LÀM MẤT VIDEO
        db.collection("videos")
                .whereEqualTo("authorId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        videoSummaries.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String thumb = document.getString("videoUri");
                            if (thumb == null || thumb.isEmpty()) {
                                thumb = document.getString("thumbnailUri");
                            }
                            videoSummaries.add(new VideoSummary(document.getString("videoId"),
                                    thumb,
                                    document.getLong("watchCount")));
                        }
                        recVideoSummary.setAdapter(new VideoSummaryAdapter(getApplicationContext(), videoSummaries));
                    }
                });
    }

    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spanCount, spacing;
        private final boolean includeEdge;
        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount; this.spacing = spacing; this.includeEdge = includeEdge;
        }
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
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

    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.imvAvatarProfile) {
            showShareAccountDialog();
        } else if (id == R.id.button_edit_profile) {
            startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class));
        } else if (id == R.id.btnBackProfile) {
            finish();
        } else if (id == R.id.button_message) {
            Intent intent = new Intent(ProfileActivity.this, ChatActivity.class);
            intent.putExtra("receiver_id", userId);
            intent.putExtra("receiver_name", txvUserName.getText().toString().replace("@", ""));
            startActivity(intent);
        } else if (id == R.id.btn_update_bio) {
            updateBio();
        } else if (id == R.id.btn_cancel_update_bio) {
            edtBio.setText(oldBioText);
        }
    }

    private void updateBio() {
        if (docRef != null && edtBio != null) {
            String newBio = edtBio.getText().toString();
            docRef.update("bio", newBio).addOnSuccessListener(aVoid -> {
                oldBioText = newBio;
                Toast.makeText(ProfileActivity.this, "Bio updated", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void showShareAccountDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.share_account_layout);
        TextView txvUsernameInSharedPlace = dialog.findViewById(R.id.txvUsernameInSharedPlace);
        ImageView imvAvatarInSharedPlace = dialog.findViewById(R.id.imvAvatarInSharedPlace);
        
        if (currentAvatarUrl != null) Glide.with(this).load(currentAvatarUrl).circleCrop().into(imvAvatarInSharedPlace);
        txvUsernameInSharedPlace.setText(txvUserName.getText());
        
        dialog.findViewById(R.id.btnCopyURL).setOnClickListener(view -> {
            ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cb.setPrimaryClip(ClipData.newPlainText("toptop-link", "http://toptoptoptop.com/" + userId));
            Toast.makeText(ProfileActivity.this, "Link copied", Toast.LENGTH_SHORT).show();
        });
        
        imvAvatarInSharedPlace.setOnClickListener(view -> startActivity(new Intent(ProfileActivity.this, FullScreenAvatarActivity.class)));
        dialog.findViewById(R.id.txvCancelInSharedPlace).setOnClickListener(view -> dialog.cancel());
        
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
        }
    }

    private void showDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_sheet_layout);
        dialog.findViewById(R.id.llSetting).setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, SettingsAndPrivacyActivity.class)));
        dialog.findViewById(R.id.llSignOut).setOnClickListener(v -> { signOut(); dialog.dismiss(); });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
        }
    }

    public void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(ProfileActivity.this, HomeScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void loadUserBio() {
        if (docRef != null) {
            docRef.get().addOnSuccessListener(document -> {
                if (document.exists() && edtBio != null) {
                    String bio = document.getString("bio");
                    if (bio != null) { oldBioText = bio; edtBio.setText(bio); }
                }
            });
        }
    }

    private void handleUnfollowed() {
        if (btn == null) return;
        btn.setText("Follow");
        btn.setOnClickListener(view -> {
            if (user == null) { startActivity(new Intent(ProfileActivity.this, MainActivity.class)); return; }
            Map<String, Object> data = new HashMap<>(); data.put("userID", userId);
            db.collection("profiles").document(currentUserID).collection("following").document(userId).set(data).addOnSuccessListener(aVoid -> {
                db.collection("profiles").document(currentUserID).update("following", FieldValue.increment(1));
                handleFollowed();
            });
            Map<String, Object> data1 = new HashMap<>(); data1.put("userID", currentUserID);
            db.collection("profiles").document(userId).collection("followers").document(currentUserID).set(data1).addOnSuccessListener(aVoid -> db.collection("profiles").document(userId).update("followers", FieldValue.increment(1)));
        });
    }

    private void handleFollowed() {
        if (btn == null) return;
        btn.setText("Unfollow");
        btn.setOnClickListener(view -> {
            if (user == null) return;
            db.collection("profiles").document(currentUserID).collection("following").document(userId).delete().addOnSuccessListener(aVoid -> {
                db.collection("profiles").document(currentUserID).update("following", FieldValue.increment(-1));
                handleUnfollowed();
            });
            db.collection("profiles").document(userId).collection("followers").document(currentUserID).delete().addOnSuccessListener(aVoid -> db.collection("profiles").document(userId).update("followers", FieldValue.increment(-1)));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileData();
    }

    public void setLikes(String userId) {
        if (userId == null) return;
        db.collection("profiles").document(userId).collection("public_videos").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ArrayList<String> userVideos = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String vidId = document.getString("videoId");
                    if (vidId != null) userVideos.add(vidId);
                }
                db.collection("likes").get().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful() && task1.getResult() != null) {
                        totalLikes = 0;
                        for (QueryDocumentSnapshot document : task1.getResult()) {
                            if (userVideos.contains(document.getId())) totalLikes += document.getData().size();
                        }
                        if (txvLikes != null) txvLikes.setText(String.valueOf(totalLikes));
                    }
                });
            }
        });
    }
}
