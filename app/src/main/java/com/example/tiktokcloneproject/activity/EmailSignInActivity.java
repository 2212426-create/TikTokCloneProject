package com.example.tiktokcloneproject.activity;

import androidx.annotation.NonNull;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.tiktokcloneproject.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class EmailSignInActivity extends Activity {

    private static final String TAG = "EmailSignInActivity";
    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_signin);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setView(R.layout.dialog_progress);
        dialog = builder.create();

        // Không tự động đăng nhập ở đây để tránh loop nếu có lỗi
        // Thay vào đó, gọi trực tiếp hộp thoại chọn tài khoản Google
        signIn();
    }

    private void signIn() {
        // Sign out trước khi sign in để đảm bảo người dùng được chọn tài khoản
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (dialog != null) dialog.show();
                handleSignIn(account);
            } catch (ApiException e) {
                Log.e(TAG, "Google sign in failed, code: " + e.getStatusCode(), e);
                String errorMsg = "Google Sign In Failed. ";
                if (e.getStatusCode() == 10) {
                    errorMsg += "Please check SHA-1 in Firebase Console.";
                } else if (e.getStatusCode() == 12500) {
                    errorMsg += "Google Play Services error.";
                } else {
                    errorMsg += "Code: " + e.getStatusCode();
                }
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                
                // Trả về màn hình cũ nhưng không dùng FLAG_ACTIVITY_CLEAR_TOP để tránh mất context
                finish();
            }
        }
    }

    private void handleSignIn(GoogleSignInAccount account) {
        if (account == null) {
            if (dialog != null) dialog.dismiss();
            finish();
            return;
        }

        // Ưu tiên đăng nhập Firebase Auth trước để người dùng vào được app nhanh nhất
        firebaseAuthWithGoogle(account);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sau khi login Firebase thành công, mới kiểm tra/tạo profile trong Firestore
                        checkAndSyncUserProfile(acct);
                    } else {
                        if (dialog != null) dialog.dismiss();
                        Log.e(TAG, "Firebase Auth failed", task.getException());
                        Toast.makeText(this, "Firebase Auth Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
    }

    private void checkAndSyncUserProfile(GoogleSignInAccount account) {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("profiles").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().exists()) {
                        // Nếu chưa có profile thì tạo mới
                        Map<String, Object> profile = new HashMap<>();
                        profile.put("userId", uid);
                        profile.put("email", account.getEmail());
                        profile.put("username", account.getDisplayName());
                        profile.put("fullname", account.getDisplayName());
                        profile.put("avatar", account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "");
                        profile.put("bio", "Toptop user");
                        
                        db.collection("profiles").document(uid).set(profile)
                                .addOnCompleteListener(t -> finishLogin());
                    } else {
                        finishLogin();
                    }
                });
    }

    private void finishLogin() {
        if (dialog != null) dialog.dismiss();
        Toast.makeText(EmailSignInActivity.this, "Sign in successful", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(EmailSignInActivity.this, HomeScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
