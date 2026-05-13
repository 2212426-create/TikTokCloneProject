package com.example.tiktokcloneproject.activity;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.adapters.VideoSummaryAdapter;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.example.tiktokcloneproject.model.Notification;
import com.example.tiktokcloneproject.model.VideoSummary;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends FragmentActivity implements View.OnClickListener {
    final String USERNAME_LABEL = "username";
    private TextView txvFollowing, txvFollowers, txvLikes, txvUserName;
    private EditText edtBio;
    private Button btn, btnEditProfile, btnUpdateBio, btnCancelUpdateBio;
    private LinearLayout llFollowing, llFollowers, llInfor;
    ImageView imvAvatarProfile;
    Uri avatarUri;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseStorage storage;
    StorageReference storageReference;
    Bitmap bitmap;
    String userId;
    DocumentReference docRef;
    String oldBioText, currentUserID;
    String TAG = "test";
    RecyclerView recVideoSummary;
    ArrayList<VideoSummary> videoSummaries;
    private int totalLikes = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Intent intent = getIntent();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        if (intent.getExtras() != null) {
            if (intent.hasExtra("id")) {
                userId = intent.getStringExtra("id");
            } else {
                String action = intent.getAction();
                Uri data = intent.getData();
                if (data != null) {
                    List<String> segmentsList = data.getPathSegments();
                    userId = segmentsList.get(segmentsList.size() - 1);
                }
            }
        } else {
            if (user != null) {
                userId = user.getUid();
            }
        }
        
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        txvFollowing = (TextView) findViewById(R.id.text_following);
        txvFollowers = (TextView) findViewById(R.id.text_followers);
        txvLikes = (TextView) findViewById(R.id.text_likes);
        txvUserName = (TextView) findViewById(R.id.txv_username);
        edtBio = (EditText) findViewById(R.id.edt_bio);
        btnEditProfile = (Button) findViewById(R.id.button_edit_profile);
        imvAvatarProfile = (ImageView) findViewById(R.id.imvAvatarProfile);
        llFollowers = (LinearLayout) findViewById(R.id.ll_followers);
        llFollowing = (LinearLayout) findViewById(R.id.ll_following);
        llInfor = (LinearLayout) findViewById(R.id.info);

        recVideoSummary = (RecyclerView) findViewById(R.id.recycle_view_video_summary);
        btnUpdateBio = (Button) findViewById(R.id.btn_update_bio);
        btnCancelUpdateBio = (Button) findViewById(R.id.btn_cancel_update_bio);

        btnUpdateBio.setOnClickListener(this);
        btnCancelUpdateBio.setOnClickListener(this);
        llFollowers.setOnClickListener(this);
        llFollowing.setOnClickListener(this);

        TextView txvMenu = (TextView) findViewById(R.id.text_menu);
        if (txvMenu != null) txvMenu.setOnClickListener(this);
        imvAvatarProfile.setOnClickListener(this);

        db = FirebaseFirestore.getInstance();
        setLikes(userId);
        docRef = db.collection("profiles").document(userId);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        
        if (user == null) {
            handleFollow();
        } else {
            currentUserID = user.getUid();
            if (userId.equals(user.getUid())) {
                btn = (Button) findViewById(R.id.button_edit_profile);
                edtBio.setVisibility(View.VISIBLE);
                btn.setVisibility(View.VISIBLE);

                loadUserBio();

                edtBio.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                    @Override
                    public void afterTextChanged(Editable editable) {}
                });
                edtBio.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean b) {
                        if (b) {
                            findViewById(R.id.layout_bio).setVisibility(View.VISIBLE);
                        } else {
                            findViewById(R.id.layout_bio).setVisibility(View.GONE);
                        }
                    }
                });
                btnEditProfile.setOnClickListener(this);
            } else {
                handleFollow();
            }
        }

        videoSummaries = new ArrayList<VideoSummary>();
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        recVideoSummary.setLayoutManager(gridLayoutManager);
        recVideoSummary.addItemDecoration(new GridSpacingItemDecoration(3, 10, true));
        setVideoSummaries();
    }

    boolean isFollowed = false;

    @Override
    public void onStart() {
        super.onStart();
        if (docRef != null) {
            docRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        txvFollowing.setText(String.valueOf(document.get("following") != null ? document.get("following") : 0));
                        txvFollowers.setText(String.valueOf(document.get("followers") != null ? document.get("followers") : 0));
                        txvLikes.setText(String.valueOf(document.get("likes") != null ? document.get("likes") : 0));
                        txvUserName.setText("@" + document.getString(USERNAME_LABEL));
                    }
                }
            });
        }
    }

    private void handleFollow() {
        btn = (Button) findViewById(R.id.button_follow);
        if (btn == null) return;
        btn.setVisibility(View.VISIBLE);

        if (user != null) {
            DocumentReference docRefFollow = db.collection("profiles").document(currentUserID)
                    .collection("following").document(userId);
            docRefFollow.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            isFollowed = true;
                            handleFollowed();
                            notifyFollow();
                        } else {
                            isFollowed = false;
                            handleUnfollowed();
                        }
                    }
                }
            });
        } else {
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intentMain = new Intent(ProfileActivity.this, MainActivity.class);
                    startActivity(intentMain);
                }
            });
        }
    }

    public void notifyFollow() {
        if (user == null) return;
        db.collection("users").document(user.getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                String username = document.get("username", String.class);
                                Notification.pushNotification(username, userId, StaticVariable.FOLLOW);
                            }
                        }
                    }
                });
    }

    protected void setVideoSummaries() {
        if (userId == null) return;
        db.collection("profiles").document(userId).collection("public_videos")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            videoSummaries.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                videoSummaries.add(new VideoSummary(document.getString("videoId"),
                                        document.getString("thumbnailUri"),
                                        document.getLong("watchCount")));
                            }
                            if (videoSummaries.size() == 0) return;
                            VideoSummaryAdapter videoSummaryAdapter = new VideoSummaryAdapter(getApplicationContext(), videoSummaries);
                            recVideoSummary.setAdapter(videoSummaryAdapter);
                        }
                    }
                });
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

    void updateBio() {
        if (docRef != null && edtBio != null) {
            String newBio = edtBio.getText().toString();
            docRef.update("bio", newBio)
                    .addOnSuccessListener(aVoid -> {
                        oldBioText = newBio;
                        Toast.makeText(ProfileActivity.this, "Bio updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ProfileActivity.this, "Failed to update bio", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    public void onClick(View v) {
        if (v.getId() == R.id.text_menu) {
            showDialog();
            return;
        }
        if (v.getId() == R.id.imvAvatarProfile) {
            showShareAccountDialog();
            return;
        }
        if (btnEditProfile != null && v.getId() == btnEditProfile.getId()) {
            moveToAnotherActivity(EditProfileActivity.class);
        }
        if (v.getId() == R.id.btnBackProfile) {
            finish();
        }
        if (btnUpdateBio != null && v.getId() == btnUpdateBio.getId()) {
            updateBio();
            findViewById(R.id.layout_bio).setVisibility(View.GONE);
            View current = getCurrentFocus();
            if (current != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(current.getWindowToken(), 0);
                current.clearFocus();
            }
        }
        if (btnCancelUpdateBio != null && v.getId() == btnCancelUpdateBio.getId()) {
            edtBio.setText(oldBioText);
            findViewById(R.id.layout_bio).setVisibility(View.GONE);
            View current = getCurrentFocus();
            if (current != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(current.getWindowToken(), 0);
                current.clearFocus();
            }
        }
        if (llFollowers != null && v.getId() == llFollowers.getId()) {
            if (currentUserID != null && currentUserID.equals(userId)) {
                Intent intent = new Intent(ProfileActivity.this, FollowListActivity.class);
                intent.putExtra("pageIndex", 1);
                startActivity(intent);
            }
        }
        if (llFollowing != null && v.getId() == llFollowing.getId()) {
            if (currentUserID != null && currentUserID.equals(userId)) {
                Intent intent = new Intent(ProfileActivity.this, FollowListActivity.class);
                intent.putExtra("pageIndex", 0);
                startActivity(intent);
            }
        }
    }

    private void showShareAccountDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.share_account_layout);
        TextView txvUsernameInSharedPlace = dialog.findViewById(R.id.txvUsernameInSharedPlace);
        ImageView imvAvatarInSharedPlace = dialog.findViewById(R.id.imvAvatarInSharedPlace);
        Button btnCopyURL = dialog.findViewById(R.id.btnCopyURL);
        TextView txvCancelInSharedPlace = dialog.findViewById(R.id.txvCancelInSharedPlace);
        if (bitmap != null) imvAvatarInSharedPlace.setImageBitmap(bitmap);
        txvUsernameInSharedPlace.setText(txvUserName.getText());
        btnCopyURL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (userId == null) return;
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("toptop-link", "http://toptoptoptop.com/" + userId);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(ProfileActivity.this, "Profile link has been saved to clipboard", Toast.LENGTH_SHORT).show();
            }
        });
        imvAvatarInSharedPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileActivity.this, FullScreenAvatarActivity.class);
                startActivity(intent);
            }
        });
        txvCancelInSharedPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });
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
        LinearLayout llSetting = dialog.findViewById(R.id.llSetting);
        LinearLayout llSignOut = dialog.findViewById(R.id.llSignOut);
        llSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileActivity.this, SettingsAndPrivacyActivity.class);
                startActivity(intent);
            }
        });
        llSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut(view);
                dialog.dismiss();
            }
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
        }
    }

    public void signOut(View v) {
        FirebaseAuth.getInstance().signOut();
        if (user != null && user.getPhoneNumber() == null) {
            try {
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();
                GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
                mGoogleSignInClient.signOut();
            } catch (Exception e) {}
        }
        Intent intent = new Intent(ProfileActivity.this, HomeScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void moveToAnotherActivity(Class<?> cls) {
        Intent intent = new Intent(ProfileActivity.this, cls);
        startActivity(intent);
    }

    private void loadUserBio() {
        if (docRef != null) {
            docRef.get().addOnSuccessListener(document -> {
                if (document.exists() && edtBio != null) {
                    String bio = document.getString("bio");
                    if (bio != null) {
                        oldBioText = bio;
                        edtBio.setText(bio);
                    }
                }
            });
        }
    }

    private void handleUnfollowed() {
        if (btn == null) return;
        btn.setText("Follow");
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (user == null) {
                    startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                    return;
                }
                if (isFollowed) return;
                isFollowed = true;
                Map<String, Object> Data = new HashMap<>();
                Data.put("userID", userId);
                db.collection("profiles").document(currentUserID)
                        .collection("following").document(userId)
                        .set(Data)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                db.collection("profiles").document(currentUserID)
                                        .update("following", FieldValue.increment(1));
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
                                db.collection("profiles").document(userId)
                                        .update("followers", FieldValue.increment(1));
                            }
                        });
            }
        });
    }

    private void handleFollowed() {
        if (btn == null) return;
        btn.setText("Unfollow");
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (user == null) return;
                if (!isFollowed) return;
                isFollowed = false;
                db.collection("profiles").document(currentUserID)
                        .collection("following").document(userId)
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                db.collection("profiles").document(currentUserID)
                                        .update("following", FieldValue.increment(-1));
                                handleUnfollowed();
                            }
                        });
                db.collection("profiles").document(userId)
                        .collection("followers").document(currentUserID)
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                db.collection("profiles").document(userId)
                                        .update("followers", FieldValue.increment(-1));
                            }
                        });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId != null) {
            // ĐÃ SỬA: Xóa dấu / ở đầu path
            StorageReference download = storageReference.child("user_avatars").child(userId);
            download.getBytes(StaticVariable.MAX_BYTES_AVATAR)
                    .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            imvAvatarProfile.setImageBitmap(bitmap);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {}
                    });
        }
    }

    public void setLikes(String userId) {
        if (userId == null) return;
        db.collection("profiles").document(userId).collection("public_videos").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    ArrayList<String> userVideos = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String vidId = document.getString("videoId");
                        if (vidId != null && !vidId.isEmpty()) userVideos.add(vidId);
                    }
                    db.collection("likes").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                totalLikes = 0;
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    if (userVideos.contains(document.getId())) {
                                        totalLikes += document.getData().size();
                                    }
                                }
                                if (txvLikes != null) txvLikes.setText(String.valueOf(totalLikes));
                            }
                        }
                    });
                }
            }
        });
    }
}
