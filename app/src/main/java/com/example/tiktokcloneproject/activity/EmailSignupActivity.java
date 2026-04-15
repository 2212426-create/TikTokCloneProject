package com.example.tiktokcloneproject.activity;

import androidx.annotation.NonNull;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.model.Profile;
import com.example.tiktokcloneproject.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class EmailSignupActivity extends Activity{

    private static final String TAG = "EmailSignUpActivity";
    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_signup); // Mở lại giao diện

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

        // Kiểm tra session cũ
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            if (dialog != null) dialog.show();
            handleSignUp(account);
        } else {
            signUp();
        }
    }

    private void signUp() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (dialog != null) dialog.show();
                handleSignUp(account);
            } catch (ApiException e) {
                Log.e(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Sign up failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                moveToAnotherActivity(SignupChoiceActivity.class);
            }
        }
    }

    private void handleSignUp(GoogleSignInAccount account) {
        db.collection("users")
                .whereEqualTo("email", account.getEmail())
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshots = task.getResult();
                        if (snapshots != null && !snapshots.isEmpty()) {
                            // Email đã tồn tại -> Chuyển sang đăng nhập
                            if (dialog != null) dialog.dismiss();
                            Toast.makeText(this, "Email already registered. Signing you in...", Toast.LENGTH_SHORT).show();
                            firebaseAuthWithGoogle(account.getIdToken());
                        } else {
                            // Email mới -> Đăng ký
                            firebaseAuthWithGoogle(account.getIdToken());
                        }
                    } else {
                        Log.e(TAG, "Firestore error during signup check", task.getException());
                        // Nếu Firestore lỗi, vẫn thử Auth để tránh treo màn hình
                        firebaseAuthWithGoogle(account.getIdToken());
                    }
                });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String id = firebaseUser.getUid();
                            String username = firebaseUser.getDisplayName();
                            if (username == null || username.isEmpty()) {
                                username = "user_" + id.substring(0, 5);
                            }
                            
                            User user = new User(id, username, "", firebaseUser.getEmail());
                            writeNewUser(user);
                            Profile profile = new Profile(id, username);
                            writeNewProfile(profile);
                        }
                        if (dialog != null) dialog.dismiss();
                        moveToAnotherActivity(HomeScreenActivity.class);
                    } else {
                        if (dialog != null) dialog.dismiss();
                        Log.e(TAG, "Firebase Auth failed", task.getException());
                        moveToAnotherActivity(SignupChoiceActivity.class);
                    }
                });
    }

    private void writeNewUser(User user) {
        db.collection("users").document(user.getUserId()).set(user.toMap());
    }

    private void writeNewProfile(Profile profile) {
        db.collection("profiles").document(profile.getUserId()).set(profile.toMap());
        
        // Tạo các collection con mặc định
        Map<String, Object> dump = new HashMap<>();
        dump.put("init", true);
        db.collection("profiles").document(profile.getUserId()).collection("following").document("dump").set(dump);
        db.collection("profiles").document(profile.getUserId()).collection("followers").document("dump").set(dump);
    }

    private void moveToAnotherActivity(Class<?> cls) {
        Intent intent = new Intent(EmailSignupActivity.this, cls);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
