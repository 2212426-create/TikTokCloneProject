package com.example.tiktokcloneproject.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.adapters.ActiveUserAdapter;
import com.example.tiktokcloneproject.adapters.NotificationAdapter;
import com.example.tiktokcloneproject.fragment.NavigationFragment;
import com.example.tiktokcloneproject.helper.FirebaseHelper;
import com.example.tiktokcloneproject.model.Notification;
import com.example.tiktokcloneproject.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InboxActivity extends FragmentActivity {
    private final String TAG = "InboxActivity";
    private DatabaseReference notificationRef;
    private DatabaseReference chatListRef;
    private ChildEventListener notificationListener;
    private ValueEventListener chatListListener;
    
    private FirebaseUser user;
    private ListView lvNotifications;
    private ArrayList<Notification> notifications;
    private ImageView btnBack;
    private RecyclerView rvActiveUsers;
    private ActiveUserAdapter activeUserAdapter;
    private List<User> activeUsers;
    private List<String> usersList;
    private Map<String, Long> userTimestamps;

    FragmentTransaction ft;
    NavigationFragment navigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rvActiveUsers = findViewById(R.id.rvActiveUsers);
        if (rvActiveUsers != null) {
            rvActiveUsers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            activeUsers = new ArrayList<>();
            activeUserAdapter = new ActiveUserAdapter(this, activeUsers);
            rvActiveUsers.setAdapter(activeUserAdapter);
        }

        lvNotifications = findViewById(R.id.lvNotifications);
        notifications = new ArrayList<>();
        ArrayAdapter<Notification> adapter = new NotificationAdapter(
                this,
                R.layout.notification_row,
                notifications);
        lvNotifications.setAdapter(adapter);

        lvNotifications.setOnItemClickListener((parent, view, position, id) -> {
            Notification notif = notifications.get(position);
            if (notif.getAction() != null && notif.getAction().startsWith("APPEAL_REQUEST")) {
                String[] parts = notif.getAction().split("\\|");
                if (parts.length > 1) {
                    String reportId = parts[1];
                    showAppealDialog(reportId);
                }
            }
        });

        ft = getSupportFragmentManager().beginTransaction();
        navigation = NavigationFragment.newInstance("navigation");
        ft.replace(R.id.flNavigation, navigation);
        ft.commit();

        user = FirebaseAuth.getInstance().getCurrentUser();
        usersList = new ArrayList<>();
        userTimestamps = new HashMap<>();
        
        if (user != null) {
            // Khởi tạo Notification Listener
            notificationRef = FirebaseHelper.getDatabase().getReference("Notifications").child(user.getUid());
            notificationListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String previousChildName) {
                    if (isFinishing()) return;
                    Notification notification = dataSnapshot.getValue(Notification.class);
                    if (notification != null) {
                        View blank = findViewById(R.id.blank_notification);
                        if (blank != null) blank.setVisibility(View.GONE);
                        adapter.insert(notification, 0);
                    }
                }
                @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
                @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                @Override public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Notification Error: " + error.getMessage());
                }
            };
            notificationRef.addChildEventListener(notificationListener);

            // Khởi tạo ChatList Listener
            chatListRef = FirebaseHelper.getDatabase().getReference("ChatList").child(user.getUid());
            chatListListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (isFinishing()) return;
                    usersList.clear();
                    userTimestamps.clear();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        String userId = dataSnapshot.getKey();
                        if (userId != null) {
                            usersList.add(userId);
                            Long ts = dataSnapshot.child("timestamp").getValue(Long.class);
                            if (ts == null) ts = dataSnapshot.child("lastTimestamp").getValue(Long.class);
                            userTimestamps.put(userId, ts != null ? ts : 0L);
                        }
                    }
                    loadActiveUsers();
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "ChatList error: " + error.getMessage());
                }
            };
            chatListRef.addValueEventListener(chatListListener);
        }
    }

    private void loadActiveUsers() {
        if (activeUsers == null || isFinishing()) return;
        if (usersList.isEmpty()) {
            activeUsers.clear();
            if (activeUserAdapter != null) activeUserAdapter.notifyDataSetChanged();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final List<User> tempActiveUsers = Collections.synchronizedList(new ArrayList<>());
        final int[] processedCount = {0};
        final int totalUsers = usersList.size();

        for (String id : usersList) {
            if ("system_admin".equals(id)) {
                handleProcessed(processedCount, totalUsers, tempActiveUsers);
                continue;
            }

            db.collection("profiles").document(id).get().addOnSuccessListener(documentSnapshot -> {
                if (isFinishing()) return;
                if (documentSnapshot.exists()) {
                    User u = new User();
                    u.setUserId(documentSnapshot.getId());
                    u.setUsername(documentSnapshot.getString("username"));
                    u.setAvatarUrl(documentSnapshot.getString("avatarUrl"));

                    FirebaseHelper.getDatabase().getReference("status").child(id)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (isFinishing()) return;
                                    Boolean isOnline = snapshot.child("online").getValue(Boolean.class);
                                    if (Boolean.TRUE.equals(isOnline)) {
                                        tempActiveUsers.add(u);
                                    }
                                    handleProcessed(processedCount, totalUsers, tempActiveUsers);
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {
                                    handleProcessed(processedCount, totalUsers, tempActiveUsers);
                                }
                            });
                } else {
                    handleProcessed(processedCount, totalUsers, tempActiveUsers);
                }
            }).addOnFailureListener(e -> handleProcessed(processedCount, totalUsers, tempActiveUsers));
        }
    }

    private void handleProcessed(int[] count, int total, List<User> tempUsers) {
        synchronized (count) {
            count[0]++;
            if (count[0] == total) {
                sortAndDisplayActiveUsers(new ArrayList<>(tempUsers));
            }
        }
    }

    private void sortAndDisplayActiveUsers(List<User> users) {
        if (isFinishing()) return;
        Collections.sort(users, (u1, u2) -> {
            Long t1 = userTimestamps.get(u1.getUserId());
            Long t2 = userTimestamps.get(u2.getUserId());
            return Long.compare(t2 != null ? t2 : 0, t1 != null ? t1 : 0);
        });

        runOnUiThread(() -> {
            if (!isFinishing()) {
                activeUsers.clear();
                activeUsers.addAll(users);
                if (activeUserAdapter != null) activeUserAdapter.notifyDataSetChanged();
            }
        });
    }

    private void showAppealDialog(String reportId) {
        FirebaseFirestore.getInstance().collection("reports").document(reportId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) return;
                String currentAppeal = documentSnapshot.getString("appeal");
                if (currentAppeal != null && !currentAppeal.isEmpty()) {
                    android.widget.Toast.makeText(this, "Bạn đã gửi kháng cáo cho báo cáo này rồi.", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                
                android.widget.EditText input = new android.widget.EditText(this);
                input.setHint("Nhập lý do kháng cáo của bạn...");
                input.setPadding(40, 40, 40, 40);
                
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Kháng cáo vi phạm")
                    .setMessage("Vui lòng giải thích để quản trị viên xem xét lại.")
                    .setView(input)
                    .setPositiveButton("Gửi", (dialog, which) -> {
                        String appealText = input.getText().toString().trim();
                        if (!appealText.isEmpty()) {
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("appeal", appealText);
                            updates.put("status", "pending");
                            
                            FirebaseFirestore.getInstance().collection("reports").document(reportId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> android.widget.Toast.makeText(InboxActivity.this, "Đã gửi kháng cáo!", android.widget.Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> android.widget.Toast.makeText(InboxActivity.this, "Lỗi khi gửi", android.widget.Toast.LENGTH_SHORT).show());
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
            });
    }

    @Override
    protected void onDestroy() {
        // Gỡ bỏ tất cả Listener để tránh crash khi Activity bị hủy
        if (notificationRef != null && notificationListener != null) {
            notificationRef.removeEventListener(notificationListener);
        }
        if (chatListRef != null && chatListListener != null) {
            chatListRef.removeEventListener(chatListListener);
        }
        super.onDestroy();
    }
}
