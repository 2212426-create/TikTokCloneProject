package com.example.tiktokcloneproject.model;

import com.example.tiktokcloneproject.helper.FirebaseHelper;
import com.google.firebase.database.FirebaseDatabase;

public class Notification {

    private String fromUsername;
    private String action;
    private long timestamp;

    public Notification() {
    }

    public Notification(String fromUser, String action) {
        this.fromUsername = fromUser;
        this.action = action;
        timestamp = System.currentTimeMillis();
    }

    public String getFromUsername() {
        return fromUsername;
    }

    public String getAction() {
        return action;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public static void pushNotification(String fromUsername, String toUserId, String action) {
        FirebaseHelper.getDatabase()
                        .getReference()
                        .child("Notifications") // Đã thêm nút Notifications để gom nhóm dữ liệu
                        .child(toUserId)
                        .push()
                        .setValue(new Notification(fromUsername, action)
                        );
    }
}
