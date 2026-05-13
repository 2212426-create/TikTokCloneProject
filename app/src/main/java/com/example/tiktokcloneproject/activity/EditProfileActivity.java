package com.example.tiktokcloneproject.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class EditProfileActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView tvUsername, tvEmail, tvBirthdate, tvSaveAvatar;
    private ImageButton imbPhoto, imbUsername, imbBirthdate;
    private LinearLayout llEmail;
    private FirebaseFirestore db;
    private Uri avatarUri;
    private ImageView imvBackToProfile;
    private Dialog dialog;
    private FirebaseStorage storage;
    
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (result.getResultCode() == RESULT_OK && data != null && data.getData() != null) {
                    avatarUri = data.getData();
                    if (imbPhoto != null) imbPhoto.setImageURI(avatarUri);
                    // Không tự động upload nữa, đợi nhấn Save
                }
            });

    private final String TAG = "EditProfileActivity";
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        storage = FirebaseStorage.getInstance("gs://mytiktokclone-f9789.firebasestorage.app");

        llEmail = findViewById(R.id.llEmail);
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        tvBirthdate = findViewById(R.id.tvBirthdate);
        tvSaveAvatar = findViewById(R.id.tvSaveAvatar);
        imbPhoto = findViewById(R.id.imbPhoto);
        imbUsername = findViewById(R.id.imbUsername);
        imbBirthdate = findViewById(R.id.imbBirthdate);
        imvBackToProfile = findViewById(R.id.imvBackToProfile);

        if (imbPhoto != null) imbPhoto.setOnClickListener(this);
        if (imvBackToProfile != null) imvBackToProfile.setOnClickListener(this);
        if (imbUsername != null) imbUsername.setOnClickListener(this);
        if (imbBirthdate != null) imbBirthdate.setOnClickListener(this);
        if (tvSaveAvatar != null) tvSaveAvatar.setOnClickListener(this);

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
            loadCurrentAvatar();
        }
    }

    private void loadUserData() {
        if (dialog != null) dialog.show();
        db.collection("users").document(user.getUid())
            .get().addOnCompleteListener(task -> {
                if (dialog != null) dialog.dismiss();
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        tvUsername.setText(getData(document.get("username")));
                        tvEmail.setText(getData(document.get("email")));
                        tvBirthdate.setText(getData(document.get("birthdate")));
                    }
                }
            });
    }

    private void loadCurrentAvatar() {
        // Sử dụng path sạch "user_avatars"
        storage.getReference().child("user_avatars").child(user.getUid()).getBytes(StaticVariable.MAX_BYTES_AVATAR)
                .addOnSuccessListener(bytes -> {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (imbPhoto != null) imbPhoto.setImageBitmap(bitmap);
                }).addOnFailureListener(e -> Log.e(TAG, "No avatar found"));
    }

    private String getData(Object data) {
        return data == null ? "" : data.toString();
    }

    private void uploadAvatar() {
        if (user == null) return;
        
        if (avatarUri == null) {
            Toast.makeText(this, "Please select a photo first", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Saving profile...");
        progress.setCancelable(false);
        progress.show();

        StorageReference uploadRef = storage.getReference().child("user_avatars").child(user.getUid());
        
        uploadRef.putFile(avatarUri)
                .addOnSuccessListener(taskSnapshot -> {
                    progress.dismiss();
                    Toast.makeText(EditProfileActivity.this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    db.collection("profiles").document(user.getUid()).update("hasAvatar", true);
                    finish(); // Quay về ProfileFragment
                })
                .addOnFailureListener(e -> {
                    progress.dismiss();
                    Log.e(TAG, "Upload failed", e);
                    Toast.makeText(EditProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
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
            uploadAvatar();
        } else if (id == R.id.imbUsername) {
            moveToEdit(StaticVariable.USERNAME, tvUsername.getText().toString());
        } else if (id == R.id.imbBirthdate) {
            moveToEdit(StaticVariable.BIRTHDATE, tvBirthdate.getText().toString());
        }
    }

    private void moveToEdit(String mode, String content) {
        Intent intent = new Intent(this, EditActivity.class);
        intent.putExtra("mode", mode);
        intent.putExtra("content", content);
        startActivity(intent);
    }
}
