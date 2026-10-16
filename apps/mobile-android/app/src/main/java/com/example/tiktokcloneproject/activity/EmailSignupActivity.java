package com.example.tiktokcloneproject.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.tiktokcloneproject.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EmailSignupActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword, edtConfirmPassword;
    private Button btnRegister;
    private ImageView btnBack;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);

        btnBack.setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
    }

    private void registerUser() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Vui lòng nhập Email!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập Mật khẩu!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải chứa ít nhất 6 ký tự!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                setupUserDataAndProfile(firebaseUser, email);
                            } else {
                                onRegistrationFailure("Lỗi xác thực người dùng mới.");
                            }
                        } else {
                            String errorMsg = task.getException() != null ? task.getException().getMessage() : "Đăng ký không thành công.";
                            onRegistrationFailure(errorMsg);
                        }
                    }
                });
    }

    private void setupUserDataAndProfile(FirebaseUser firebaseUser, String email) {
        String uid = firebaseUser.getUid();
        
        // Generate a clean username from the email handle
        String username = email.split("@")[0];
        username = username.replaceAll("[^a-zA-Z0-9_.]", "");
        if (username.isEmpty()) {
            username = "user_" + uid.substring(0, 5);
        }
        
        final String finalUsername = username;

        // Check and sync user document
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", uid);
        userData.put("username", finalUsername);
        userData.put("birthdate", "");
        userData.put("avatarUrl", "");
        userData.put("email", email);
        userData.put("isPrivate", false);
        userData.put("phone", "");
        userData.put("status", "active");
        userData.put("role", "user");

        db.collection("users").document(uid).set(userData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> userTask) {
                        if (userTask.isSuccessful()) {
                            // Check and create profile document
                            Map<String, Object> profileData = new HashMap<>();
                            profileData.put("userId", uid);
                            profileData.put("email", email);
                            profileData.put("username", finalUsername);
                            profileData.put("fullname", finalUsername);
                            profileData.put("avatar", "");
                            profileData.put("bio", "Toptop user");
                            profileData.put("followers", 0);
                            profileData.put("following", 0);
                            profileData.put("likes", 0);
                            profileData.put("isPrivate", false);

                            db.collection("profiles").document(uid).set(profileData)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> profileTask) {
                                            progressBar.setVisibility(View.GONE);
                                            btnRegister.setEnabled(true);
                                            if (profileTask.isSuccessful()) {
                                                Toast.makeText(EmailSignupActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(EmailSignupActivity.this, HomeScreenActivity.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                Toast.makeText(EmailSignupActivity.this, "Lỗi đồng bộ thông tin hồ sơ: " + 
                                                        (profileTask.getException() != null ? profileTask.getException().getMessage() : ""), Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });
                        } else {
                            progressBar.setVisibility(View.GONE);
                            btnRegister.setEnabled(true);
                            Toast.makeText(EmailSignupActivity.this, "Lỗi đồng bộ dữ liệu người dùng: " + 
                                    (userTask.getException() != null ? userTask.getException().getMessage() : ""), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void onRegistrationFailure(String message) {
        progressBar.setVisibility(View.GONE);
        btnRegister.setEnabled(true);
        Toast.makeText(EmailSignupActivity.this, "Đăng ký thất bại: " + message, Toast.LENGTH_LONG).show();
    }
}
