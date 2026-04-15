package com.example.tiktokcloneproject.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class SplashScreenActivity extends Activity {

    private final int SPLASH_DISPLAY_LENGTH = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Có thể set layout nếu có file res/layout/activity_splash_screen.xml
        // setContentView(R.layout.activity_splash_screen);

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                try {
                    Intent mainIntent = new Intent(SplashScreenActivity.this, HomeScreenActivity.class);
                    startActivity(mainIntent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                } catch (Exception e) {
                    Log.e("SplashError", "Lỗi khi chuyển màn hình: " + e.getMessage());
                    // Nếu lỗi nghiêm trọng, cố gắng mở màn hình chính mà không có animation
                    Intent intent = new Intent(SplashScreenActivity.this, HomeScreenActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
}
