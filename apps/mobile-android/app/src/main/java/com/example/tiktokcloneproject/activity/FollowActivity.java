package com.example.tiktokcloneproject.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class FollowActivity extends Activity {
    private TextView txvFollowing, txvFollowers, txvLikes, txvUserName;
    private Button btn, btnMessage;
    private ImageView imvAvatarProfile, btnBack;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    FirebaseStorage storage;
    StorageReference storageReference;
    Bitmap bitmap;
    String currentUserID, userId;
    boolean isFollowed;
    private ListenerRegistration profileListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow);

        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            finish();
            return;
        }
        User user = (User) bundle.get("obj");
        if (user == null) {
            finish();
            return;
        }

        imvAvatarProfile = findViewById(R.id.imvAvatarProfile);
        btnBack = findViewById(R.id.btnBack);
        txvUserName = findViewById(R.id.txv_username);
        txvUserName.setText(user.getUsername());
        txvFollowing = findViewById(R.id.text_following);
        txvFollowers = findViewById(R.id.text_followers);
        txvLikes = findViewById(R.id.text_likes);
        btn = findViewById(R.id.button_follow);
        btnMessage = findViewById(R.id.btnMessage);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        userId = user.getUserId();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        
        if (currentUser == null) {
            btnMessage.setVisibility(View.GONE);
            btn.setVisibility(View.GONE);
        } else {
            currentUserID = currentUser.getUid();
            if (currentUserID.equals(userId)) {
                btnMessage.setVisibility(View.GONE);
                btn.setVisibility(View.GONE);
            }
        }

        db = FirebaseFirestore.getInstance();

        if (currentUser != null) {
            DocumentReference docRef = db.collection("profiles").document(currentUserID)
                    .collection("following").document(userId);
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            isFollowed = true;
                            handleFollowed();
                        } else {
                            isFollowed = false;
                            handleUnfollowed();
                        }
                    }
                }
            });
        }

        btnMessage.setOnClickListener(v -> {
            Intent intent = new Intent(FollowActivity.this, ChatActivity.class);
            intent.putExtra("receiver_id", userId);
            intent.putExtra("receiver_name", user.getUsername());
            startActivity(intent);
        });
    }

    private void handleUnfollowed() {
        btn.setText("Follow");
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentUser == null) {
                    Intent intent = new Intent(FollowActivity.this, MainActivity.class);
                    startActivity(intent);
                    return;
                }
                Map<String, Object> Data = new HashMap<>();
                Data.put("userID", userId);

                db.collection("profiles").document(currentUserID)
                        .collection("following").document(userId)
                        .set(Data)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                syncFollowCounts(currentUserID);
                                handleFollowed();
                            }
                        });

                Map<String, Object> Data1 = new HashMap<>();
                Data1.put("userID", currentUserID);
                db.collection("profiles").document(userId)
                        .collection("followers").document(currentUserID)
                        .set(Data1)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                syncFollowCounts(userId);
                                
                                // Gửi thông báo Follow
                                db.collection("profiles").document(currentUserID).get().addOnSuccessListener(doc -> {
                                    if (doc.exists()) {
                                        String name = doc.getString("username");
                                        com.example.tiktokcloneproject.model.Notification.pushNotification(
                                            name != null ? name : "Ai đó", 
                                            userId, 
                                            com.example.tiktokcloneproject.helper.StaticVariable.FOLLOW
                                        );
                                    }
                                });
                            }
                        });
            }
        });
    }

    private void handleFollowed() {
        btn.setText("Unfollow");
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentUser == null) return;
                db.collection("profiles").document(currentUserID)
                        .collection("following").document(userId)
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                syncFollowCounts(currentUserID);
                                handleUnfollowed();
                            }
                        });

                db.collection("profiles").document(userId)
                        .collection("followers").document(currentUserID)
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                syncFollowCounts(userId);
                            }
                        });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadProfileData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
    }

    private void loadProfileData() {
        if (userId != null && !userId.isEmpty()) {
            syncFollowCounts(userId);
        }
        DocumentReference docRef = db.collection("profiles").document(userId);
        profileListener = docRef.addSnapshotListener((document, e) -> {
            if (e != null) {
                return;
            }
            if (document != null && document.exists()) {
                if (txvFollowing != null) {
                    txvFollowing.setText(String.valueOf(document.get("following") != null ? document.get("following") : 0));
                }
                if (txvFollowers != null) {
                    txvFollowers.setText(String.valueOf(document.get("followers") != null ? document.get("followers") : 0));
                }
                if (txvLikes != null) {
                    txvLikes.setText(String.valueOf(document.get("likes") != null ? document.get("likes") : 0));
                }
                
                String avatarUrl = document.getString("avatarUrl");
                if (avatarUrl != null && !avatarUrl.isEmpty() && imvAvatarProfile != null) {
                    Glide.with(this)
                            .load(avatarUrl)
                            .placeholder(R.drawable.default_avatar)
                            .circleCrop()
                            .into(imvAvatarProfile);
                }
            }
        });
    }

    private void syncFollowCounts(String uid) {
        if (uid == null || uid.isEmpty()) return;
        db.collection("profiles").document(uid).collection("followers").get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int count = queryDocumentSnapshots != null ? queryDocumentSnapshots.size() : 0;
                db.collection("profiles").document(uid).update("followers", count);
            });
        db.collection("profiles").document(uid).collection("following").get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int count = queryDocumentSnapshots != null ? queryDocumentSnapshots.size() : 0;
                db.collection("profiles").document(uid).update("following", count);
            });
    }

    public void onClick(View v) {
        if (v.getId() == R.id.imvAvatarProfile) {
            // Handle avatar click
        }
    }
}
