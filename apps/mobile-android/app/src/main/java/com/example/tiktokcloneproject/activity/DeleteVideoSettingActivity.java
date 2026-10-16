package com.example.tiktokcloneproject.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.tiktokcloneproject.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class DeleteVideoSettingActivity extends AppCompatActivity {

    private ImageView imvBackToVideo;
    private FrameLayout flDeleteVideo;

    private String videoId;
    private String authorVideoId;

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String TAG = "DeleteVideoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_video_setting);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        imvBackToVideo = findViewById(R.id.imvBackToVideo);
        flDeleteVideo = findViewById(R.id.flDeleteVideo);

        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            videoId = intent.getExtras().getString("videoId");
            authorVideoId = intent.getExtras().getString("authorId");
        }

        imvBackToVideo.setOnClickListener(view -> {
            onBackPressed();
            finish();
        });

        flDeleteVideo.setOnClickListener(view -> {
            if (videoId == null) {
                Toast.makeText(this, "Video ID not found", Toast.LENGTH_SHORT).show();
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(DeleteVideoSettingActivity.this);
            builder.setMessage("Are you sure you want to delete this video?");
            builder.setCancelable(true);

            builder.setPositiveButton("Delete", (dialog, id) -> {
                startDeletionProcess();
                dialog.cancel();
            });
            builder.setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());

            AlertDialog alert = builder.create();
            alert.show();
            alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.RED);
        });
    }

    private void startDeletionProcess() {
        // 1. Cố gắng xóa file trên Storage nếu có, sau đó tiếp tục xóa Firestore
        db.collection("videos").document(videoId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String videoUri = task.getResult().getString("videoUri");
                if (videoUri != null && videoUri.contains("firebasestorage.googleapis.com")) {
                    try {
                        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(videoUri);
                        ref.delete().addOnCompleteListener(storageTask -> {
                            if (!storageTask.isSuccessful()) Log.e(TAG, "Storage delete failed");
                            proceedFirestoreDeletion();
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing storage URL", e);
                        proceedFirestoreDeletion();
                    }
                } else {
                    proceedFirestoreDeletion();
                }
            } else {
                proceedFirestoreDeletion();
            }
        });
    }

    private void proceedFirestoreDeletion() {
        // 2. Xóa các Hashtags liên quan (tìm theo videoId)
        db.collection("hashtags").whereEqualTo("videoId", videoId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            db.collection("hashtags").document(doc.getId()).delete();
                        }
                    }
                    deleteVideoDocuments();
                });
    }

    private void deleteVideoDocuments() {
        // 3. Xóa document chính và các bản tóm tắt
        db.collection("videos").document(videoId).delete();
        db.collection("video_summaries").document(videoId).delete();
        
        if (authorVideoId != null) {
            db.collection("profiles").document(authorVideoId)
                    .collection("public_videos").document(videoId).delete();
        }

        // 4. Xóa likes và comments
        db.collection("likes").document(videoId).delete();
        db.collection("comments").document(videoId).delete().addOnCompleteListener(task -> {
            Toast.makeText(DeleteVideoSettingActivity.this, "Delete successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(DeleteVideoSettingActivity.this, HomeScreenActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }
}
