package com.example.tiktokcloneproject.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.view.View;
import android.os.Bundle;
import android.widget.ImageView;

import com.example.tiktokcloneproject.R;

public class LoginOptionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_options);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    public void emailLogInPage(View view) {
        Intent intent = new Intent(LoginOptionsActivity.this, EmailLogInActivity.class);
        startActivity(intent);
    }
}