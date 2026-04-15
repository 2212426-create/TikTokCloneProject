package com.example.tiktokcloneproject.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class EditProfileActivity extends Activity implements View.OnClickListener {
    private TextView tvUsername, tvPhone, tvEmail, tvBirthdate;
    private ImageButton imbPhoto, imbSelect, imbUsername, imbBirthdate;
    private LinearLayout llEditProfile, llChangePhoto, llPhone, llEmail;
    private FirebaseFirestore db;
    private Uri avatarUri;
    private final int SELECT_IMAGE_CODE = 10;
    private ImageView imvBackToProfile;
    private Dialog dialog;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    private final String TAG = "EditProfileActivity";
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        llEditProfile = findViewById(R.id.llEditProfile);
        llChangePhoto = findViewById(R.id.llChangePhoto);
        llPhone = findViewById(R.id.llPhone);
        llEmail = findViewById(R.id.llEmail);
        tvUsername = findViewById(R.id.tvUsername);
        tvPhone = findViewById(R.id.tvPhone);
        tvEmail = findViewById(R.id.tvEmail);
        tvBirthdate = findViewById(R.id.tvBirthdate);
        imbPhoto = findViewById(R.id.imbPhoto);
        imbSelect = findViewById(R.id.imbSelect);
        imbUsername = findViewById(R.id.imbUsername);
        imbBirthdate = findViewById(R.id.imbBirthdate);
        imvBackToProfile = findViewById(R.id.imvBackToProfile);

        if (imbSelect != null) imbSelect.setVisibility(View.GONE);

        imbPhoto.setOnClickListener(this);
        if (imbSelect != null) imbSelect.setOnClickListener(this);
        imvBackToProfile.setOnClickListener(this);
        imbUsername.setOnClickListener(this);
        imbBirthdate.setOnClickListener(this);

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
            String phoneNumber = user.getPhoneNumber();
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                llPhone.setVisibility(View.GONE);
                llEmail.setVisibility(View.VISIBLE);
            } else {
                llPhone.setVisibility(View.VISIBLE);
                llEmail.setVisibility(View.GONE);
            }

            if (dialog != null) dialog.show();

            db.collection("users").document(user.getUid())
                .get().addOnCompleteListener(task -> {
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }

                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            tvUsername.setText(getData(document.get("username")));
                            tvPhone.setText(getData(document.get("phone")));
                            tvEmail.setText(getData(document.get("email")));
                            tvBirthdate.setText(getData(document.get("birthdate")));
                        }
                    } else {
                        Log.e(TAG, "Fetch failed", task.getException());
                        Toast.makeText(this, "Cannot connect to server", Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }

    private String getData(Object data) {
        return data == null ? "" : data.toString();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_IMAGE_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            avatarUri = data.getData();
            uploadAvatar();
        }
    }

    private void uploadAvatar() {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Uploading...");
        progress.setCancelable(false);
        progress.show();

        StorageReference upload = storageReference.child("/user_avatars").child(user.getUid());
        upload.putFile(avatarUri)
                .addOnSuccessListener(taskSnapshot -> {
                    progress.dismiss();
                    Toast.makeText(this, "Image Uploaded", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progress.dismiss();
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.imbPhoto) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Photo"), SELECT_IMAGE_CODE);
        } else if (id == R.id.imvBackToProfile) {
            finish();
        } else if (id == R.id.imbUsername) {
            moveToEdit(StaticVariable.USERNAME, tvUsername.getText().toString());
        } else if (id == R.id.imbBirthdate) {
            moveToEdit(StaticVariable.BIRTHDATE, tvBirthdate.getText().toString());
        }
    }

    private void moveToEdit(String mode, String content) {
        Intent intent = new Intent(this, EditActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("mode", mode);
        bundle.putString("content", content);
        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }
}
