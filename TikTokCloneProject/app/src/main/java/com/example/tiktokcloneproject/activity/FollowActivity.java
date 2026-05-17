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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class FollowActivity extends Activity {
    private TextView txvFollowing, txvFollowers, txvLikes, txvUserName;
    private Button btn, btnMessage;
    private ImageView imvAvatarProfile, btnBack;
    Uri avatarUri;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    FirebaseStorage storage;
    StorageReference storageReference;
    Bitmap bitmap;
    String currentUserID, userId;
    boolean isFollowed;

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
                Map<String, Object> Data = new HashMap<>();
                Data.put("userID", userId);

                db.collection("profiles").document(currentUserID)
                        .collection("following").document(userId)
                        .set(Data)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                handleFollowed();
                            }
                        });

                Map<String, Object> Data1 = new HashMap<>();
                Data1.put("userID", currentUserID);
                db.collection("profiles").document(userId)
                        .collection("followers").document(currentUserID)
                        .set(Data1);
            }
        });
    }

    private void handleFollowed() {
        btn.setText("Unfollow");
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                db.collection("profiles").document(currentUserID)
                        .collection("following").document(userId)
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                handleUnfollowed();
                            }
                        });

                db.collection("profiles").document(userId)
                        .collection("followers").document(currentUserID)
                        .delete();
            }
        });
    }

    public void onClick(View v) {
        if (v.getId() == R.id.imvAvatarProfile) {
            // Handle avatar click
        }
    }
}
