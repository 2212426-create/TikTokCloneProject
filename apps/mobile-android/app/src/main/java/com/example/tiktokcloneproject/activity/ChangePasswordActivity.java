package com.example.tiktokcloneproject.activity;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.helper.Validator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChangePasswordActivity extends FragmentActivity implements View.OnClickListener {

    final Integer GONE = View.GONE;
    final Integer VISIBLE = View.VISIBLE;
    Fragment fragmentWaiting;
    Validator validator;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseUser user;
    Handler handler = new Handler(Looper.getMainLooper());
    String phone;
    String password;
    String msg;
    private LinearLayout llChangePassword, llOldPassword, llNewPassword;
    private FragmentTransaction ft;
    private FragmentManager fm;
    private EditText edtOldPassword, edtNewPassword, edtConfirmPassword;
    private Button btnOldPassword, btnNewPassword;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        llChangePassword = (LinearLayout) findViewById(R.id.llChangePassword);
        llOldPassword = (LinearLayout) llChangePassword.findViewById(R.id.llOldPassword);
        llNewPassword = (LinearLayout) llChangePassword.findViewById(R.id.llNewPassword);
        edtOldPassword = (EditText) llChangePassword.findViewById(R.id.edtOldPassword);
        edtNewPassword = (EditText) llChangePassword.findViewById(R.id.edtNewPassword);
        edtConfirmPassword = (EditText) llChangePassword.findViewById(R.id.edtConfirmPassword);
        btnOldPassword = (Button) llChangePassword.findViewById(R.id.btnOldPassword);
        btnNewPassword = (Button) llChangePassword.findViewById(R.id.btnNewPassword);
        btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        fragmentWaiting = (Fragment) getSupportFragmentManager().findFragmentById(R.id.fragWaiting);

        validator = Validator.getInstance();
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, getString(R.string.error_verify), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fm = getSupportFragmentManager();

        addShowHideListener(fragmentWaiting);

        setVisibleVisibility(llOldPassword.getId());

        btnOldPassword.setOnClickListener(this);
        btnNewPassword.setOnClickListener(this);
    }

    private void setVisibleVisibility(Integer id) {
        llNewPassword.setVisibility(GONE);
        llOldPassword.setVisibility(GONE);

        findViewById(id).setVisibility(VISIBLE);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == btnOldPassword.getId()) {
            password = edtOldPassword.getText().toString();
            if (password.isEmpty() || !validator.isValidPassword(password)) {
                Toast.makeText(ChangePasswordActivity.this, getString(R.string.error_Password), Toast.LENGTH_SHORT).show();
            } else {
                phone = user.getPhoneNumber();
                if (phone == null || phone.isEmpty()) {
                    // Fallback to getting phone from Firestore if not in Auth
                    db.collection("users").document(user.getUid()).get().addOnSuccessListener(documentSnapshot -> {
                        String firestorePhone = documentSnapshot.getString("phone");
                        if (firestorePhone != null) {
                            verifyOldPassword(firestorePhone, password);
                        } else {
                            Toast.makeText(this, getString(R.string.error_change_password_not_supported), Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }
                verifyOldPassword(phone, password);
            }
        }
        if (view.getId() == btnNewPassword.getId()) {
            String newPassword = edtNewPassword.getText().toString();
            String confirm = edtConfirmPassword.getText().toString();
            if (newPassword.isEmpty() || !validator.isValidPassword(newPassword)) {
                Toast.makeText(this, getString(R.string.error_Password), Toast.LENGTH_SHORT).show();
            } else if (!confirm.equals(newPassword)) {
                Toast.makeText(this, getString(R.string.error_confirm), Toast.LENGTH_SHORT).show();
            } else {
                addShowHideListener(fragmentWaiting);
                db.collection("users").document(user.getUid()).update("password", newPassword)
                    .addOnSuccessListener(aVoid -> {
                        addShowHideListener(fragmentWaiting);
                        Toast.makeText(this, getString(R.string.successful_changePassword), Toast.LENGTH_SHORT).show();
                        // FIX: Chỉ gọi finish() để quay về màn hình trước đó thay vì khởi tạo lại Activity mới
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        addShowHideListener(fragmentWaiting);
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                    });
            }
        }
    }

    private void verifyOldPassword(String phoneNumber, String oldPassword) {
        addShowHideListener(fragmentWaiting);
        db.collection("users")
                .whereEqualTo("phone", phoneNumber)
                .whereEqualTo("password", oldPassword)
                .get().addOnCompleteListener(task -> {
                    addShowHideListener(fragmentWaiting);
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        setVisibleVisibility(llNewPassword.getId());
                    } else {
                        Toast.makeText(this, getString(R.string.error_old_password), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    void addShowHideListener(final Fragment fragment) {
        if (fragment == null) return;
        ft = fm.beginTransaction();
        ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
        if (fragment.isHidden()) {
            ft.show(fragment);
        } else {
            ft.hide(fragment);
        }
        ft.commit();
    }
}
