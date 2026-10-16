package com.example.tiktokcloneproject.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.tiktokcloneproject.R;
import com.google.android.material.button.MaterialButton;

public class SignupChoiceActivity extends Activity implements View.OnClickListener {
    MaterialButton btnChoiceEmail, btnChoiceEmailPassword;
    TextView txvTitle, txvAlt;
    ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_choice);

        btnChoiceEmail = (MaterialButton) findViewById(R.id.btnChoiceEmail);
        btnChoiceEmail.setIconTint(null);
        btnChoiceEmailPassword = (MaterialButton) findViewById(R.id.btnChoiceEmailPassword);
        txvTitle = (TextView) findViewById(R.id.txvTitle);
        txvAlt = (TextView) findViewById(R.id.txv_alternative);
        btnBack = findViewById(R.id.btnBack);

        txvTitle.setText(getString(R.string.sign_up));
        txvAlt.setText(getString(R.string.sign_up_alt));

        btnChoiceEmail.setOnClickListener(this);
        btnChoiceEmailPassword.setOnClickListener(this);
        txvAlt.setOnClickListener(this);
        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == btnChoiceEmail.getId()) {
            Intent intent = new Intent(SignupChoiceActivity.this, EmailSignInActivity.class);
            startActivity(intent);
        }
        if(view.getId() == btnChoiceEmailPassword.getId()) {
            Intent intent = new Intent(SignupChoiceActivity.this, EmailSignupActivity.class);
            startActivity(intent);
        }
        if(view.getId() == txvAlt.getId()) {
            Intent intent = new Intent(SignupChoiceActivity.this, EmailLogInActivity.class);
            startActivity(intent);
        }
    }
}
