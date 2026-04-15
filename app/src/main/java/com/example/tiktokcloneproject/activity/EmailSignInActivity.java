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

        // Kiểm tra nếu đã login Google rồi thì dùng luôn, không bắt chọn lại
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            if (dialog != null) dialog.show();
            handleSignIn(account);
        } else {
            signIn();
        }
    }

    private void signIn() {
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
                handleSignIn(account);
            } catch (ApiException e) {
                Log.e(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google Sign In Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                moveToAnotherActivity(SigninChoiceActivity.class);
            }
        }
    }

    private void handleSignIn(GoogleSignInAccount account) {
        Log.d(TAG, "Checking user in Firestore: " + account.getEmail());
        
        // Timeout check: Nếu sau 10s không thấy Firestore phản hồi, tự động thử đăng nhập Firebase luôn
        db.collection("users").whereEqualTo("email", account.getEmail())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshots = task.getResult();
                        if (snapshots != null && !snapshots.isEmpty()) {
                            Log.d(TAG, "User exists, signing in...");
                            firebaseAuthWithGoogle(account.getIdToken());
                        } else {
                            Log.d(TAG, "User does not exist, creating new profile...");
                            createNewUser(account);
                        }
                    } else {
                        // Lỗi Firestore (thường là do chưa bật API hoặc Permission)
                        Log.e(TAG, "Firestore error: ", task.getException());
                        // Dù lỗi Firestore vẫn cho phép Firebase Auth chạy để người dùng vào được app
                        firebaseAuthWithGoogle(account.getIdToken());
                    }
                });
    }

    private void createNewUser(GoogleSignInAccount account) {
        Map<String, Object> user = new HashMap<>();
        user.put("email", account.getEmail());
        user.put("username", account.getDisplayName());
        user.put("profileImageUrl", account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "");
        user.put("userId", account.getId());

        db.collection("users").document(account.getId()).set(user)
                .addOnCompleteListener(task -> firebaseAuthWithGoogle(account.getIdToken()));
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (dialog != null) dialog.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(EmailSignInActivity.this, "Sign in successful", Toast.LENGTH_SHORT).show();
                        moveToAnotherActivity(HomeScreenActivity.class);
                    } else {
                        Log.e(TAG, "Firebase Auth failed", task.getException());
                        Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                        moveToAnotherActivity(SigninChoiceActivity.class);
                    }
                });
    }

    private void moveToAnotherActivity(Class<?> cls) {
        Intent intent = new Intent(EmailSignInActivity.this, cls);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
