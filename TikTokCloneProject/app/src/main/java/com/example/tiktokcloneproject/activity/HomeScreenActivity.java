package com.example.tiktokcloneproject.activity;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.fragment.InboxFragment;
import com.example.tiktokcloneproject.fragment.ProfileFragment;
import com.example.tiktokcloneproject.fragment.SearchFragment;
import com.example.tiktokcloneproject.fragment.VideoFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeScreenActivity extends FragmentActivity implements View.OnClickListener{

    VideoFragment videoFragment;
    SearchFragment searchFragment;
    ProfileFragment profileFragment;
    InboxFragment inboxFragment;
    private long pressedTime;
    private Button btnHome, btnAddVideo, btnInbox, btnProfile, btnSearch;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private final static String TAG = "HomeScreenActivity";
    Intent fragmentIntent = null;
    Boolean openAppFromLink = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_TikTokCloneProject);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);
        fragmentIntent = getIntent();

        if (fragmentIntent != null && fragmentIntent.getExtras() != null) {
            if (fragmentIntent.hasExtra("id")) {
                openAppFromLink = true;
            }
        }

        btnHome = findViewById(R.id.btnHome);
        btnAddVideo = findViewById(R.id.btnAddVideo);
        btnInbox = findViewById(R.id.btnInbox);
        btnProfile = findViewById(R.id.btnProfile);
        btnSearch = findViewById(R.id.btnSearch);

        btnHome.setOnClickListener(this);
        btnAddVideo.setOnClickListener(this);
        btnInbox.setOnClickListener(this);
        btnProfile.setOnClickListener(this);
        btnSearch.setOnClickListener(this);

        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        // Initial fragment load
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (fragmentIntent != null && fragmentIntent.getExtras() != null) {
            if (fragmentIntent.hasExtra("fragment_inbox")) {
                inboxFragment = InboxFragment.newInstance("inbox");
                ft.add(R.id.main_fragment, inboxFragment, "inbox");
            } else if (fragmentIntent.hasExtra("fragment_profile")) {
                profileFragment = ProfileFragment.newInstance("profile", "");
                ft.add(R.id.main_fragment, profileFragment, "profile");
            } else if (fragmentIntent.hasExtra("fragment_search")) {
                searchFragment = SearchFragment.newInstance("search");
                ft.add(R.id.main_fragment, searchFragment, "search");
            } else {
                videoFragment = VideoFragment.newInstance("fragment_video");
                ft.add(R.id.main_fragment, videoFragment, "video");
            }
        } else {
            videoFragment = VideoFragment.newInstance("fragment_video");
            ft.add(R.id.main_fragment, videoFragment, "video");
        }
        ft.commit();

    }//on Create

    @Override
    public void onBackPressed() {
        if (pressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
            finish();
        } else {
            Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT).show();
        }
        pressedTime = System.currentTimeMillis();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoFragment != null) videoFragment.pauseVideo();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btnSearch) {
            handleSearchClick();
        } else if (id == R.id.btnProfile) {
            handleProfileClick();
        } else if (id == R.id.btnAddVideo) {
            handleAddClick();
        } else if (id == R.id.btnHome) {
            handleHomeClick();
        } else if (id == R.id.btnInbox) {
            handleInboxClick();
        }
    }

    private void handleSearchClick() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (searchFragment == null) {
            searchFragment = SearchFragment.newInstance("search");
            ft.add(R.id.main_fragment, searchFragment, "search");
        }
        showFragments(ft, 1);
        ft.commit();
    }

    private void handleProfileClick() {
        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user == null) {
            Intent intent = new Intent(this, SignupChoiceActivity.class);
            startActivity(intent);
            return;
        }

        if (openAppFromLink) {
            Intent intent = new Intent(this, ProfileActivity.class);
            if (fragmentIntent != null && fragmentIntent.getExtras() != null) {
                intent.putExtras(fragmentIntent.getExtras());
            }
            startActivity(intent);
        } else {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            if (profileFragment == null) {
                profileFragment = ProfileFragment.newInstance("profile", "");
                ft.add(R.id.main_fragment, profileFragment, "profile");
            }
            showFragments(ft, 3);
            ft.commit();
        }
    }

    private void handleAddClick() {
        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user == null) {
            showNiceDialogBox(this, null, null);
            return;
        }
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_right_to_left, R.anim.fade_in);
    }

    private void handleInboxClick() {
        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user == null) {
            showNiceDialogBox(this, null, null);
            return;
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (inboxFragment == null) {
            inboxFragment = InboxFragment.newInstance("inbox");
            ft.add(R.id.main_fragment, inboxFragment, "inbox");
        }
        showFragments(ft, 2);
        ft.commit();
    }

    private void handleHomeClick() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (videoFragment == null) {
            videoFragment = VideoFragment.newInstance("fragment_video");
            ft.add(R.id.main_fragment, videoFragment, "video");
        }
        showFragments(ft, 0);
        ft.commit();
    }

    private void showNiceDialogBox(Context context, @Nullable String title, @Nullable String message) {
        if(title == null) {
            title = getString(R.string.request_account_title);
        }
        if(message == null) {
            message = getString(R.string.request_account_message);
        }
        try {
            AlertDialog.Builder myBuilder = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
            myBuilder.setIcon(R.drawable.splash_background)
                    .setTitle(title)
                    .setMessage(message)
                    .setNegativeButton("Cancel", (dialogInterface, i) -> {
                        if(!(context instanceof HomeScreenActivity)) {
                            Intent intent = new Intent(context, HomeScreenActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        }
                    })
                    .setPositiveButton("Sign up/Sign in", (dialog, whichOne) -> {
                        Intent intent = new Intent(context, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    })
                    .show();
        }
        catch (Exception e) { Log.e("Error DialogBox", e.getMessage() ); }
    }

    private void showFragments(FragmentTransaction ft, int position) {
        if (videoFragment != null) {
            if (position == 0) {
                ft.show(videoFragment);
                videoFragment.continueVideo();
            } else {
                ft.hide(videoFragment);
                videoFragment.pauseVideo();
            }
        }
        if (searchFragment != null) {
            if (position == 1) ft.show(searchFragment);
            else ft.hide(searchFragment);
        }
        if (inboxFragment != null) {
            if (position == 2) ft.show(inboxFragment);
            else ft.hide(inboxFragment);
        }
        if (profileFragment != null) {
            if (position == 3) ft.show(profileFragment);
            else ft.hide(profileFragment);
        }
    }
}
