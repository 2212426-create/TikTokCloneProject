package com.example.tiktokcloneproject.fragment;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tiktokcloneproject.activity.EditProfileActivity;
import com.example.tiktokcloneproject.activity.FollowListActivity;
import com.example.tiktokcloneproject.activity.FullScreenAvatarActivity;
import com.example.tiktokcloneproject.activity.HomeScreenActivity;
import com.example.tiktokcloneproject.activity.MainActivity;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.activity.SettingsAndPrivacyActivity;
import com.example.tiktokcloneproject.adapters.VideoSummaryAdapter;
import com.example.tiktokcloneproject.helper.StaticVariable;
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
import java.util.Map;

public class ProfileFragment extends Fragment implements View.OnClickListener {
    final String USERNAME_LABEL = "username";
    private Context context = null;
    private TextView txvFollowing, txvFollowers, txvLikes, txvUserName, txvMenu;
    private EditText edtBio;
    private Button btn, btnEditProfile, btnUpdateBio, btnCancelUpdateBio;
    private LinearLayout llFollowing, llFollowers;
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
    View layout;
    int totalLikes = 0;

    public static ProfileFragment newInstance(String strArg, String profileLinkId) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("name", strArg);
        args.putString("id", profileLinkId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        Bundle idBundle = getArguments();
        if (idBundle != null) {
            userId = idBundle.getString("id", "");
        }

        if (userId == null || userId.isEmpty()) {
            if (user != null) {
                userId = user.getUid();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layout = inflater.inflate(R.layout.fragment_profile, container, false);

        txvFollowing = layout.findViewById(R.id.text_following);
        txvFollowers = layout.findViewById(R.id.text_followers);
        txvLikes = layout.findViewById(R.id.text_likes);
        txvUserName = layout.findViewById(R.id.txv_username);
        txvMenu = layout.findViewById(R.id.text_menu);
        edtBio = layout.findViewById(R.id.edt_bio);
        btnEditProfile = layout.findViewById(R.id.button_edit_profile);
        imvAvatarProfile = layout.findViewById(R.id.imvAvatarProfile);
        llFollowers = layout.findViewById(R.id.ll_followers);
        llFollowing = layout.findViewById(R.id.ll_following);
        recVideoSummary = layout.findViewById(R.id.recycle_view_video_summary);
        btnUpdateBio = layout.findViewById(R.id.btn_update_bio);
        btnCancelUpdateBio = layout.findViewById(R.id.btn_cancel_update_bio);

        if (btnUpdateBio != null) btnUpdateBio.setOnClickListener(this);
        if (btnCancelUpdateBio != null) btnCancelUpdateBio.setOnClickListener(this);
        if (llFollowers != null) llFollowers.setOnClickListener(this);
        if (llFollowing != null) llFollowing.setOnClickListener(this);
        if (txvMenu != null) txvMenu.setOnClickListener(this);
        if (imvAvatarProfile != null) imvAvatarProfile.setOnClickListener(this);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        db = FirebaseFirestore.getInstance();

        if (user != null) {
            currentUserID = user.getUid();
            setLikes(currentUserID);
        }

        if (userId != null && !userId.isEmpty()) {
            docRef = db.collection("profiles").document(userId);
            if (user != null && userId.equals(user.getUid())) {
                if (btnEditProfile != null) {
                    btnEditProfile.setVisibility(View.VISIBLE);
                    btnEditProfile.setOnClickListener(this);
                }
                if (edtBio != null) edtBio.setVisibility(View.VISIBLE);
            } else {
                handleFollow();
            }
        }

        videoSummaries = new ArrayList<>();
        if (recVideoSummary != null) {
            recVideoSummary.setLayoutManager(new GridLayoutManager(context != null ? context : getActivity(), 3));
            recVideoSummary.addItemDecoration(new GridSpacingItemDecoration(3, 10, true));
        }
        setVideoSummaries();

        return layout;
    }

    private void handleFollow() {
        btn = layout.findViewById(R.id.button_follow);
        if (btn == null) return;

        btn.setVisibility(View.VISIBLE);
        if (userId == null || userId.isEmpty()) return;

        if (docRef != null) {
            docRef.get().addOnCompleteListener(task -> {
                if (isAdded() && task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        if (txvFollowing != null)
                            txvFollowing.setText(String.valueOf(document.get("following") != null ? document.get("following") : 0));
                        if (txvFollowers != null)
                            txvFollowers.setText(String.valueOf(document.get("followers") != null ? document.get("followers") : 0));
                        if (txvLikes != null)
                            txvLikes.setText(String.valueOf(document.get("likes") != null ? document.get("likes") : 0));
                        if (txvUserName != null) txvUserName.setText("@" + document.getString(USERNAME_LABEL));
                    }
                }
            });
        }

        if (user != null && !userId.equals(currentUserID)) {
            db.collection("profiles").document(currentUserID)
                    .collection("following").document(userId).get().addOnCompleteListener(task -> {
                if (isAdded() && task.isSuccessful() && task.getResult() != null) {
                    if (task.getResult().exists()) {
                        handleFollowed();
                    } else {
                        handleUnfollowed();
                    }
                }
            });
        } else if (user == null) {
            btn.setOnClickListener(view -> {
                Intent intentMain = new Intent(context != null ? context : getActivity(), MainActivity.class);
                startActivity(intentMain);
            });
        }
    }

    protected void setVideoSummaries() {
        if (userId == null || userId.isEmpty()) return;

        db.collection("profiles").document(userId).collection("public_videos")
                .get()
                .addOnCompleteListener(task -> {
                    if (isAdded() && task.isSuccessful() && task.getResult() != null) {
                        videoSummaries.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            videoSummaries.add(new VideoSummary(document.getString("videoId"),
                                    document.getString("thumbnailUri"),
                                    document.getLong("watchCount")));
                        }
                        if (videoSummaries.size() > 0 && recVideoSummary != null) {
                            VideoSummaryAdapter videoSummaryAdapter = new VideoSummaryAdapter(context != null ? context : getActivity(), videoSummaries);
                            recVideoSummary.setAdapter(videoSummaryAdapter);
                        }
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (docRef != null) {
            docRef.get().addOnCompleteListener(task -> {
                if (isAdded() && task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        if (txvFollowing != null)
                            txvFollowing.setText(String.valueOf(document.get("following") != null ? document.get("following") : 0));
                        if (txvFollowers != null)
                            txvFollowers.setText(String.valueOf(document.get("followers") != null ? document.get("followers") : 0));
                        if (txvLikes != null)
                            txvLikes.setText(String.valueOf(document.get("likes") != null ? document.get("likes") : 0));
                        if (txvUserName != null) txvUserName.setText("@" + document.getString(USERNAME_LABEL));
                        oldBioText = document.getString("bio");
                        if (edtBio != null) edtBio.setText(oldBioText);
                    }
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.text_menu) {
            showDialog();
        } else if (id == R.id.imvAvatarProfile) {
            showShareAccountDialog();
        } else if (id == R.id.button_edit_profile) {
            moveToAnotherActivity(EditProfileActivity.class);
        } else if (id == R.id.btn_update_bio) {
            updateBio();
        } else if (id == R.id.btn_cancel_update_bio) {
            if (edtBio != null) edtBio.setText(oldBioText);
        } else if (id == R.id.ll_followers) {
            Intent intent = new Intent(context != null ? context : getActivity(), FollowListActivity.class);
            intent.putExtra("pageIndex", 1);
            startActivity(intent);
        } else if (id == R.id.ll_following) {
            Intent intent = new Intent(context != null ? context : getActivity(), FollowListActivity.class);
            intent.putExtra("pageIndex", 0);
            startActivity(intent);
        }
    }

    void updateBio() {
        if (docRef != null && edtBio != null) {
            docRef.update("bio", edtBio.getText().toString());
            oldBioText = edtBio.getText().toString();
        }
    }

    private void showShareAccountDialog() {
        final Dialog dialog = new Dialog(context != null ? context : getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.share_account_layout);

        TextView txvUsernameInSharedPlace = dialog.findViewById(R.id.txvUsernameInSharedPlace);
        ImageView imvAvatarInSharedPlace = dialog.findViewById(R.id.imvAvatarInSharedPlace);
        Button btnCopyURL = dialog.findViewById(R.id.btnCopyURL);
        TextView txvCancelInSharedPlace = dialog.findViewById(R.id.txvCancelInSharedPlace);

        if (bitmap != null && imvAvatarInSharedPlace != null)
            imvAvatarInSharedPlace.setImageBitmap(bitmap);
        if (txvUsernameInSharedPlace != null && txvUserName != null)
            txvUsernameInSharedPlace.setText(txvUserName.getText());

        if (btnCopyURL != null) {
            btnCopyURL.setOnClickListener(view -> {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("toptop-link", "http://toptoptoptop.com/" + userId);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context != null ? context : getActivity(), "Profile link copied", Toast.LENGTH_SHORT).show();
            });
        }

        if (txvCancelInSharedPlace != null) txvCancelInSharedPlace.setOnClickListener(view -> dialog.cancel());

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
        }
    }

    private void showDialog() {
        final Dialog dialog = new Dialog(context != null ? context : getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_sheet_layout);

        View llSetting = dialog.findViewById(R.id.llSetting);
        if (llSetting != null) {
            llSetting.setOnClickListener(view -> {
                startActivity(new Intent(context != null ? context : getActivity(), SettingsAndPrivacyActivity.class));
            });
        }

        View llSignOut = dialog.findViewById(R.id.llSignOut);
        if (llSignOut != null) {
            llSignOut.setOnClickListener(view -> {
                FirebaseAuth.getInstance().signOut();
                if (getActivity() != null) getActivity().finish();
                startActivity(new Intent(context != null ? context : getActivity(), HomeScreenActivity.class));
            });
        }

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
        }
    }

    private void moveToAnotherActivity(Class<?> cls) {
        startActivity(new Intent(context != null ? context : getActivity(), cls));
    }

    private void handleUnfollowed() {
        if (btn == null) return;
        btn.setText("Follow");
        btn.setOnClickListener(view -> {
            if (currentUserID == null || userId == null) return;
            Map<String, Object> data = new HashMap<>();
            data.put("userID", userId);
            db.collection("profiles").document(currentUserID).collection("following").document(userId).set(data)
                    .addOnSuccessListener(aVoid -> {
                        db.collection("profiles").document(currentUserID).update("following", FieldValue.increment(1));
                        handleFollowed();
                    });
        });
    }

    private void handleFollowed() {
        if (btn == null) return;
        btn.setText("Unfollow");
        btn.setOnClickListener(view -> {
            if (currentUserID == null || userId == null) return;
            db.collection("profiles").document(currentUserID).collection("following").document(userId).delete()
                    .addOnSuccessListener(aVoid -> {
                        db.collection("profiles").document(currentUserID).update("following", FieldValue.increment(-1));
                        handleUnfollowed();
                    });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userId != null && !userId.isEmpty() && storageReference != null) {
            storageReference.child("/user_avatars").child(userId).getBytes(StaticVariable.MAX_BYTES_AVATAR)
                    .addOnSuccessListener(bytes -> {
                        if (isAdded()) {
                            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            if (imvAvatarProfile != null) imvAvatarProfile.setImageBitmap(bitmap);
                        }
                    });
            setVideoSummaries();
        }
    }

    public void setLikes(String userId) {
        if (userId == null || userId.isEmpty()) return;
        db.collection("profiles").document(userId).collection("public_videos").get().addOnCompleteListener(task -> {
            if (isAdded() && task.isSuccessful() && task.getResult() != null) {
                ArrayList<String> userVideos = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    userVideos.add(document.getString("videoId"));
                }
                if (txvLikes != null) txvLikes.setText(String.valueOf(totalLikes));
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
}
