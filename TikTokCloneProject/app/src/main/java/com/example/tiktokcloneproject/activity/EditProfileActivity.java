package com.example.tiktokcloneproject.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView tvUsername, tvEmail, tvBirthdate, tvSaveAvatar;
    private CircleImageView imbPhoto; // Đã đổi thành CircleImageView
    private ImageView imbUsername, imbBirthdate;
    private LinearLayout llEmail;
    private FirebaseFirestore db;
    private Uri avatarUri;
    private ImageView imvBackToProfile;
    private Dialog dialog;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    avatarUri = result.getData().getData();
                    if (imbPhoto != null) {
                        // Hiển thị ảnh tạm thời vừa chọn
                        Glide.with(this).load(avatarUri).placeholder(R.drawable.default_avatar).into(imbPhoto);
                    }
                }
            });

    private final String TAG = "EditProfileActivity";
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        llEmail = findViewById(R.id.llEmail);
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        tvBirthdate = findViewById(R.id.tvBirthdate);
        tvSaveAvatar = findViewById(R.id.tvSaveAvatar);
        imbPhoto = findViewById(R.id.imbPhoto); // CircleImageView trong XML
        imbUsername = findViewById(R.id.imbUsername);
        imbBirthdate = findViewById(R.id.imbBirthdate);
        imvBackToProfile = findViewById(R.id.imvBackToProfile);

        if (imbPhoto != null) imbPhoto.setOnClickListener(this);
        if (imvBackToProfile != null) imvBackToProfile.setOnClickListener(this);
        if (findViewById(R.id.rlUsername) != null) findViewById(R.id.rlUsername).setOnClickListener(this);
        if (findViewById(R.id.rlBirthdate) != null) findViewById(R.id.rlBirthdate).setOnClickListener(this);
        if (tvSaveAvatar != null) tvSaveAvatar.setOnClickListener(this);

        if (llEmail != null) llEmail.setOnClickListener(this);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setView(R.layout.dialog_progress);
        dialog = builder.create();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (user != null) {
            loadUserData();
        }
    }

    private void loadUserData() {
        if (dialog != null) dialog.show();
        db.collection("users").document(user.getUid())
            .get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        tvUsername.setText(getData(document.get("username")));
                        tvEmail.setText(getData(document.get("email")));
                        tvBirthdate.setText(getData(document.get("birthdate")));
                    }
                }
                
                // Chỉ load avatar từ server nếu người dùng chưa chọn ảnh mới
                if (avatarUri == null) {
                    db.collection("profiles").document(user.getUid()).get().addOnSuccessListener(doc -> {
                        if (dialog != null) dialog.dismiss();
                        if (doc.exists()) {
                            String avatarUrl = doc.getString("avatarUrl");
                            if (avatarUrl != null && avatarUri == null) { // Double check
                                Glide.with(this)
                                     .load(avatarUrl)
                                     .placeholder(R.drawable.default_avatar)
                                     .into(imbPhoto);
                            }
                        }
                    }).addOnFailureListener(e -> {
                        if (dialog != null) dialog.dismiss();
                    });
                } else {
                    if (dialog != null) dialog.dismiss();
                }
            });
    }

    private String getData(Object data) {
        return data == null ? "" : data.toString();
    }

    private void validateAndSave() {
        String username = tvUsername.getText().toString().trim();
        if (username.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        saveProfileInfo(username);
    }

    private void saveProfileInfo(String username) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Saving changes...");
        progress.setCancelable(false);
        progress.show();

        // Cập nhật cả users và profiles để đồng bộ
        Map<String, Object> nameUpdate = new HashMap<>();
        nameUpdate.put("username", username);

        db.collection("users").document(user.getUid())
            .update("username", username)
            .addOnSuccessListener(aVoid -> {
                db.collection("profiles").document(user.getUid()).set(nameUpdate, SetOptions.merge())
                    .addOnSuccessListener(aVoid2 -> {
                        if (avatarUri != null) {
                            uploadAvatarToCloudinary(progress);
                        } else {
                            progress.dismiss();
                            Toast.makeText(EditProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
            })
            .addOnFailureListener(e -> {
                progress.dismiss();
                Toast.makeText(EditProfileActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void uploadAvatarToCloudinary(ProgressDialog progress) {
        MediaManager.get().upload(avatarUri)
                .unsigned("toptopclone")
                .option("resource_type", "image")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        progress.setMessage("Uploading photo to Cloudinary...");
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        
                        Map<String, Object> update = new HashMap<>();
                        update.put("avatarUrl", imageUrl);
                        update.put("hasAvatar", true);
                        
                        db.collection("profiles").document(user.getUid())
                                .set(update, SetOptions.merge())
                                .addOnCompleteListener(task -> {
                                    db.collection("users").document(user.getUid()).update("avatarUrl", imageUrl);
                                    progress.dismiss();
                                    Toast.makeText(EditProfileActivity.this, "Avatar updated successfully!", Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        progress.dismiss();
                        Log.e(TAG, "Cloudinary Error: " + error.getDescription());
                        Toast.makeText(EditProfileActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.imbPhoto) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            pickImageLauncher.launch(Intent.createChooser(intent, "Select Photo"));
        } else if (id == R.id.imvBackToProfile) {
            finish();
        } else if (id == R.id.tvSaveAvatar) {
            validateAndSave();
        } else if (id == R.id.rlUsername) {
            moveToEdit(StaticVariable.USERNAME, tvUsername.getText().toString());
        } else if (id == R.id.rlBirthdate) {
            moveToEdit(StaticVariable.BIRTHDATE, tvBirthdate.getText().toString());
        } else if (id == R.id.llEmail) {
            startActivity(new Intent(this, ChangePasswordActivity.class));
        }
    }

    private void moveToEdit(String mode, String content) {
        Intent intent = new Intent(this, EditActivity.class);
        intent.putExtra("mode", mode);
        intent.putExtra("content", content);
        startActivity(intent);
    }
}
