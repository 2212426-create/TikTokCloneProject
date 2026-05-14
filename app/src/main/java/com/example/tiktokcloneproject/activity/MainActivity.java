package com.example.tiktokcloneproject.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.example.tiktokcloneproject.R;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    public void signUpPage(View v) {
        Intent intent = new Intent(MainActivity.this, SignupChoiceActivity.class);
        startActivity(intent);
    }

    public void signInPage(View v) {
        Intent intent = new Intent(MainActivity.this, SigninChoiceActivity.class);
        startActivity(intent);
    }
}