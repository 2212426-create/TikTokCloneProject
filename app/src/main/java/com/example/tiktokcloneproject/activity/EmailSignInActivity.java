package com.example.tiktokcloneproject.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EmailSignInActivity extends AppCompatActivity {

    private static final String TAG = "EmailSignInActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private Dialog dialog;
    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (data == null) {
                    if (dialog != null) dialog.dismiss();
                    finish();
                    return;
                }
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
                    finish();
                }
            });

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
            googleSignInLauncher.launch(signInIntent);
        });
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
                        checkAndSyncUserDataAndProfile(acct);
                    } else {
                        if (dialog != null) dialog.dismiss();
                        Log.e(TAG, "Firebase Auth failed", task.getException());
                        Toast.makeText(this, "Firebase Auth Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
    }

    private void checkAndSyncUserDataAndProfile(GoogleSignInAccount account) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            if (dialog != null) dialog.dismiss();
            Toast.makeText(this, "Authentication session is invalid. Please try again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String uid = currentUser.getUid();
        String username = account.getDisplayName();
        if (username == null || username.isEmpty()) {
            username = "user_" + uid.substring(0, 5);
        }
        String email = account.getEmail() != null ? account.getEmail() : currentUser.getEmail();
        final String finalUsername = username;
        final String finalEmail = email;

        db.collection("users").document(uid).get().addOnCompleteListener(userTask -> {
            if (!userTask.isSuccessful()) {
                if (dialog != null) dialog.dismiss();
                Log.e(TAG, "Fetch user document failed", userTask.getException());
                Toast.makeText(this, "Cannot sync user data", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            if (userTask.getResult() == null || !userTask.getResult().exists()) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("userId", uid);
                userData.put("username", finalUsername);
                userData.put("birthdate", "");
                userData.put("avatarUri", "");
                userData.put("email", finalEmail == null ? "" : finalEmail);
                userData.put("isPrivate", false);
                userData.put("phone", "");

                db.collection("users").document(uid).set(userData)
                        .addOnCompleteListener(createUserTask -> {
                            if (!createUserTask.isSuccessful()) {
                                if (dialog != null) dialog.dismiss();
                                Log.e(TAG, "Create user document failed", createUserTask.getException());
                                Toast.makeText(this, "Cannot sync user data", Toast.LENGTH_LONG).show();
                                finish();
                                return;
                            }
                            checkAndCreateProfile(uid, finalUsername, finalEmail, account);
                        });
            } else {
                checkAndCreateProfile(uid, finalUsername, finalEmail, account);
            }
        });
    }

    private void checkAndCreateProfile(String uid, String username, String email, GoogleSignInAccount account) {
        db.collection("profiles").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        if (dialog != null) dialog.dismiss();
                        Log.e(TAG, "Fetch profile document failed", task.getException());
                        Toast.makeText(this, "Cannot sync profile data", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    if (task.getResult() != null && !task.getResult().exists()) {
                        Map<String, Object> profile = new HashMap<>();
                        profile.put("userId", uid);
                        profile.put("email", email == null ? "" : email);
                        profile.put("username", username);
                        profile.put("fullname", username);
                        profile.put("avatar", account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "");
                        profile.put("bio", "Toptop user");
                        profile.put("followers", 0);
                        profile.put("following", 0);
                        profile.put("likes", 0);
                        profile.put("isPrivate", false);

                        db.collection("profiles").document(uid).set(profile)
                                .addOnCompleteListener(t -> {
                                    if (t.isSuccessful()) {
                                        finishLogin();
                                    } else {
                                        if (dialog != null) dialog.dismiss();
                                        Log.e(TAG, "Create profile failed", t.getException());
                                        Toast.makeText(this, "Cannot sync profile data", Toast.LENGTH_LONG).show();
                                        finish();
                                    }
                                });
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
