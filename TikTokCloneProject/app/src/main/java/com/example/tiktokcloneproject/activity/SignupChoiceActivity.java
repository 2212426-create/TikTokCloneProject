package com.example.tiktokcloneproject.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.tiktokcloneproject.R;

public class SignupChoiceActivity extends Activity implements View.OnClickListener {
    Button btnChoiceEmail;
    LinearLayout llSignupChoice;
    TextView txvTitle, txvAlt;
    ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_choice);

        llSignupChoice = (LinearLayout) findViewById(R.id.llSignupChoice);
        btnChoiceEmail = (Button) llSignupChoice.findViewById(R.id.btnChoiceEmail);
        txvTitle = (TextView) llSignupChoice.findViewById(R.id.txvTitle);
        txvAlt = (TextView) llSignupChoice.findViewById(R.id.txv_alternative);
        btnBack = findViewById(R.id.btnBack);

        txvTitle.setText(getString(R.string.sign_up));
        txvAlt.setText(getString(R.string.sign_up_alt));

        btnChoiceEmail.setOnClickListener(this);
        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == btnChoiceEmail.getId()) {
            Intent intent = new Intent(SignupChoiceActivity.this, EmailSignupActivity.class);
            startActivity(intent);
        }
        if(view.getId() == txvAlt.getId()) {
            Intent intent = new Intent(SignupChoiceActivity.this, SigninChoiceActivity.class);
            startActivity(intent);
        }
    }
}
