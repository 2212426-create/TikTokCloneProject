package com.example.tiktokcloneproject.activity;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.adapters.CommentAdapter;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.example.tiktokcloneproject.model.Comment;
import com.example.tiktokcloneproject.model.Notification;
import com.example.tiktokcloneproject.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Map;

public class CommentActivity extends Activity implements View.OnClickListener{
    private ImageView imvBack, imvMyAvatarInComment;
    private LinearLayout llComment;
    private EditText edtComment;
    private ImageButton imbSendComment;
    private String videoId, userId;
    private Bitmap bitmap;
    private ListView lvComment;
    FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseFirestore db;
    FirebaseStorage storage;
    StorageReference storageRef, imagesRef;
    DocumentReference docRef;
    String username;
    String authorVideoId;
    int totalComments;
    CommentAdapter adapter;

    private LinearLayout llReplyPreview;
    private TextView txvReplyingTo;
    private ImageView imvCancelReply;
    private Comment replyingToComment = null;
    private String replyingToUsername = null;

    Handler handler = new Handler(Looper.getMainLooper());

    ArrayList<Comment> comments;
    ArrayList<Comment> allComments = new ArrayList<>();
    private HashSet<String> expandedComments = new HashSet<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);


        llComment = (LinearLayout) findViewById(R.id.llComment);
        imvBack = (ImageView) llComment.findViewById(R.id.imvBackToHomeScreen);
        imvMyAvatarInComment = (ImageView)llComment.findViewById(R.id.imvMyAvatarInComment);
        edtComment = (EditText) llComment.findViewById(R.id.edtComment);
        imbSendComment = (ImageButton) llComment.findViewById(R.id.imbSendComment);
        lvComment = (ListView) llComment.findViewById(R.id.listViewComment);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        videoId = bundle.getString("videoId");
        authorVideoId = bundle.getString("authorId");
        totalComments = bundle.getInt("totalComments");

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        comments = new ArrayList<>();
        adapter = new CommentAdapter(this, R.layout.layout_row_comment, comments);
        lvComment.setAdapter(adapter);

        llReplyPreview = findViewById(R.id.llReplyPreview);
        txvReplyingTo = findViewById(R.id.txvReplyingTo);
        imvCancelReply = findViewById(R.id.imvCancelReply);
        if (imvCancelReply != null) {
            imvCancelReply.setOnClickListener(v -> cancelReply());
        }




        imvBack.setOnClickListener(this);
        imbSendComment.setOnClickListener(this);



        if (user != null) {
            db.collection("users").document(user.getUid())
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    username = document.get("username", String.class);
                                    Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                                } else {
                                    Log.d(TAG, "No such document");
                                }
                            } else {
                                Log.d(TAG, "get failed with ", task.getException());
                            }
                        }
                    });
        }

        db.collection("comments")
                .whereEqualTo("videoId", videoId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            Comment comment = dc.getDocument().toObject(Comment.class);
                            int index = findCommentIndex(comment.getCommentId());
                            
                            switch (dc.getType()) {
                                case ADDED:
                                    if (index == -1) {
                                        allComments.add(comment);
                                    }
                                    break;
                                case MODIFIED:
                                    if (index != -1) {
                                        allComments.set(index, comment);
                                    }
                                    break;
                                case REMOVED:
                                    if (index != -1) {
                                        allComments.remove(index);
                                    }
                                    break;
                            }
                        }
                        sortComments();
                        adapter.notifyDataSetChanged();
                    }
                });

        if (user != null) {
            userId = user.getUid();
            loadCurrentUserAvatar(userId);
        }
        else
        {
            Intent intent1 = new Intent(CommentActivity.this, com.example.tiktokcloneproject.activity.MainActivity.class);
            startActivity(intent1);
            finish();
        }
    }

    private void loadCurrentUserAvatar(String uid) {
        FirebaseFirestore.getInstance().collection("profiles").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String avatarUrl = doc.getString("avatarUrl");
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(this)
                                .load(avatarUrl)
                                .placeholder(R.drawable.default_avatar)
                                .circleCrop()
                                .into(imvMyAvatarInComment);
                    } else {
                        loadAvatarFromStorage(uid);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Load profile avatar failed: " + e.getMessage());
                    loadAvatarFromStorage(uid);
                });
    }

    private void loadAvatarFromStorage(String uid) {
        StorageReference download = storageRef.child("user_avatars").child(uid);
        download.getBytes(StaticVariable.MAX_BYTES_AVATAR)
                .addOnSuccessListener(bytes -> {
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    imvMyAvatarInComment.setImageBitmap(bitmap);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Load storage avatar failed: " + e.getMessage()));
    }

    private int findCommentIndex(String id) {
        for (int i = 0; i < allComments.size(); i++) {
            if (allComments.get(i).getCommentId().equals(id)) return i;
        }
        return -1;
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    public void onClick(View v){
        if (v.getId() == imvBack.getId()){
            onBackPressed();
        }
        if (v.getId() == imbSendComment.getId()){
            String cmt = edtComment.getText().toString().trim();
            if (TextUtils.isEmpty(cmt))
            {
                return;
            }
            String timeStamp = String.valueOf(System.currentTimeMillis());
            Comment comment = new Comment(timeStamp, videoId, userId, cmt);
            if (replyingToComment != null) {
                // If replying to a reply, group under the top-level parent comment
                if (replyingToComment.getParentId() != null && !replyingToComment.getParentId().isEmpty()) {
                    comment.setParentId(replyingToComment.getParentId());
                } else {
                    comment.setParentId(replyingToComment.getCommentId());
                }
                comment.setParentUsername(replyingToUsername);
            }
            postComment(comment);
            edtComment.setText("");
        }
    }

    private void postComment(Comment comment ) {
        db.collection("comments").document(comment.getCommentId()).set(comment).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    Notification.pushNotification(username, authorVideoId, StaticVariable.COMMENT);
                    
                    if (comment.getParentId() != null && !comment.getParentId().isEmpty() && replyingToComment != null) {
                        db.collection("comments").document(comment.getParentId())
                                .update("totalReplies", FieldValue.increment(1),
                                        "replyIds", FieldValue.arrayUnion(comment.getCommentId()));
                        if (!comment.getAuthorId().equals(replyingToComment.getAuthorId())) {
                            Notification.pushNotification(username, replyingToComment.getAuthorId(), StaticVariable.COMMENT);
                        }
                    }
                    
                    handler.post(CommentActivity.this::updateTotal);
                    cancelReply();
                }
                else{
                    Toast.makeText(CommentActivity.this, "Comment fail!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void startReply(Comment comment, String authorUsername) {
        this.replyingToComment = comment;
        this.replyingToUsername = authorUsername;
        if (llReplyPreview != null) llReplyPreview.setVisibility(View.VISIBLE);
        if (txvReplyingTo != null) txvReplyingTo.setText("@" + authorUsername + "...");
        if (edtComment != null) {
            edtComment.setHint("@" + authorUsername + "...");
            edtComment.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(edtComment, 0);
            }
        }
    }

    public void cancelReply() {
        this.replyingToComment = null;
        this.replyingToUsername = null;
        if (llReplyPreview != null) llReplyPreview.setVisibility(View.GONE);
        if (edtComment != null) edtComment.setHint("Add comment...");
    }

    private void sortComments() {
        ArrayList<Comment> parents = new ArrayList<>();
        HashMap<String, ArrayList<Comment>> repliesMap = new HashMap<>();
        
        for (Comment c : allComments) {
            if (c.getParentId() == null || c.getParentId().isEmpty()) {
                parents.add(c);
            } else {
                String pId = c.getParentId();
                if (!repliesMap.containsKey(pId)) {
                    repliesMap.put(pId, new ArrayList<>());
                }
                repliesMap.get(pId).add(c);
            }
        }
        
        Collections.sort(parents, (c1, c2) -> Long.compare(Long.parseLong(c2.getCommentId()), Long.parseLong(c1.getCommentId())));
        
        ArrayList<Comment> sorted = new ArrayList<>();
        for (Comment parent : parents) {
            sorted.add(parent);
            ArrayList<Comment> replies = repliesMap.get(parent.getCommentId());
            if (replies != null && !replies.isEmpty()) {
                Collections.sort(replies, (c1, c2) -> Long.compare(Long.parseLong(c1.getCommentId()), Long.parseLong(c2.getCommentId())));
                
                String parentId = parent.getCommentId();
                if (expandedComments.contains(parentId) || replies.size() <= 2) {
                    sorted.addAll(replies);
                    if (replies.size() > 2) {
                        Comment collapseDummy = new Comment(parentId + "_collapse", videoId, "", "");
                        collapseDummy.setParentId(parentId);
                        sorted.add(collapseDummy);
                    }
                } else {
                    sorted.add(replies.get(0));
                    sorted.add(replies.get(1));
                    
                    Comment expandDummy = new Comment(parentId + "_expand", videoId, "", "");
                    expandDummy.setParentId(parentId);
                    expandDummy.setTotalReplies(replies.size());
                    sorted.add(expandDummy);
                }
            }
        }
        
        comments.clear();
        comments.addAll(sorted);
    }

    public void expandComment(String parentId) {
        expandedComments.add(parentId);
        sortComments();
        adapter.notifyDataSetChanged();
    }

    public void collapseComment(String parentId) {
        expandedComments.remove(parentId);
        sortComments();
        adapter.notifyDataSetChanged();
    }

    private void updateTotal() {
        totalComments += 1;
        db.collection("videos").document(videoId)
                .update("totalComments", FieldValue.increment(1))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully updated!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating document", e);
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.slide_left_to_right, R.anim.slide_out_bottom);
    }
}
