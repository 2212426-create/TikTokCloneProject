package com.example.tiktokcloneproject.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.adapters.NotificationAdapter;
import com.example.tiktokcloneproject.fragment.NavigationFragment;
import com.example.tiktokcloneproject.helper.FirebaseHelper;
import com.example.tiktokcloneproject.model.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;

public class InboxActivity extends FragmentActivity {
    private final String TAG = "InboxActivity";
    private DatabaseReference mDatabase = null;
    private FirebaseUser user;
    private ListView lvNotifications;
    private ArrayList<Notification> notifications;
    private ImageView btnBack;

    FragmentTransaction ft;
    NavigationFragment navigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        lvNotifications = (ListView) findViewById(R.id.lvNotifications);
        notifications = new ArrayList<>();
        ArrayAdapter<Notification> adapter = new NotificationAdapter(
                this,
                R.layout.notification_row,
                notifications);
        lvNotifications.setAdapter(adapter);

        ft = getSupportFragmentManager().beginTransaction();
        navigation = NavigationFragment.newInstance("navigation");
        ft.replace(R.id.flNavigation, navigation);
        ft.commit();

        user = FirebaseAuth.getInstance().getCurrentUser();
        
        // Sử dụng FirebaseHelper để lấy đúng instance database
        mDatabase = FirebaseHelper.getDatabase().getReference("Notifications");

        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildAdded:" + dataSnapshot.getKey());

                Notification notification = dataSnapshot.getValue(Notification.class);
                if (notification != null) {
                    View blank = findViewById(R.id.blank_notification);
                    if (blank != null) blank.setVisibility(View.GONE);
                    adapter.insert(notification, 0);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Notification Error: " + error.getMessage());
            }
        };

        if (user != null) {
            mDatabase.child(user.getUid()).addChildEventListener(childEventListener);
        }
    }
}
