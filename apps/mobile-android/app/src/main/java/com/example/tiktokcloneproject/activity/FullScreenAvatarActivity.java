package com.example.tiktokcloneproject.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FullScreenAvatarActivity extends AppCompatActivity{

    ImageView imvFullScreen;
    FirebaseStorage storage;
    StorageReference storageReference;
    FirebaseUser user;
    Bitmap bitmap;
    String folderPath;
    String fileName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_avatar);
        ActionBar actionBar = getSupportActionBar();
        user = FirebaseAuth.getInstance().getCurrentUser();

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        if (actionBar!=null) {
            actionBar.hide();
        }

        imvFullScreen = (ImageView) findViewById(R.id.imvFullscreen);

        getImage();
        imvFullScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void getImage() {
        String avatarUrl = getIntent().getStringExtra("avatarUrl");
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.default_avatar)
                    .into(imvFullScreen);
        } else {
            // Fallback: load current user avatar from Firestore
            if (user != null) {
                FirebaseFirestore.getInstance().collection("profiles").document(user.getUid()).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String url = documentSnapshot.getString("avatarUrl");
                                if (url != null && !url.isEmpty()) {
                                    Glide.with(FullScreenAvatarActivity.this)
                                            .load(url)
                                            .placeholder(R.drawable.default_avatar)
                                            .into(imvFullScreen);
                                    return;
                                }
                            }
                            imvFullScreen.setImageResource(R.drawable.default_avatar);
                        })
                        .addOnFailureListener(e -> {
                            imvFullScreen.setImageResource(R.drawable.default_avatar);
                        });
            } else {
                imvFullScreen.setImageResource(R.drawable.default_avatar);
            }
        }
    }

}
