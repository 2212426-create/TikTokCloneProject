package com.example.tiktokcloneproject.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.adapters.ChatAdapter;
import com.example.tiktokcloneproject.helper.FirebaseHelper;
import com.example.tiktokcloneproject.model.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private ImageView btnBackChat, ivUserChat;
    private TextView tvUserNameChat;
    private RecyclerView rvChat;
    private EditText etMessage;
    private ImageButton btnSendMessage;

    private FirebaseUser fUser;
    private DatabaseReference reference;

    private ChatAdapter messageAdapter;
    private List<ChatMessage> mChat;

    private String receiverId;
    private String receiverName;
    private String roomId;
    private final String TAG = "ChatActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        receiverId = getIntent().getStringExtra("receiver_id");
        receiverName = getIntent().getStringExtra("receiver_name");

        if (receiverId == null) {
            Toast.makeText(this, "Không tìm thấy ID người nhận", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để nhắn tin", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        roomId = getRoomId(fUser.getUid(), receiverId);

        btnBackChat = findViewById(R.id.btnBackChat);
        ivUserChat = findViewById(R.id.ivUserChat);
        tvUserNameChat = findViewById(R.id.tvUserNameChat);
        rvChat = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        btnSendMessage = findViewById(R.id.btnSendMessage);

        tvUserNameChat.setText(receiverName != null ? receiverName : "Người dùng");
        btnBackChat.setOnClickListener(v -> finish());

        // Lấy thông tin avatar từ Firestore
        FirebaseFirestore.getInstance().collection("profiles").document(receiverId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String avatarUrl = doc.getString("avatarUrl");
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(this).load(avatarUrl).placeholder(R.drawable.default_avatar).into(ivUserChat);
                            if (messageAdapter != null) messageAdapter.setImageUrl(avatarUrl);
                        }
                    }
                });

        rvChat.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(linearLayoutManager);

        mChat = new ArrayList<>();
        messageAdapter = new ChatAdapter(ChatActivity.this, mChat);
        rvChat.setAdapter(messageAdapter);

        btnSendMessage.setOnClickListener(v -> {
            String msg = etMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(msg)) {
                sendMessage(fUser.getUid(), receiverId, msg);
                etMessage.setText("");
            }
        });

        readMessages();
    }

    private String getRoomId(String id1, String id2) {
        return (id1.compareTo(id2) < 0) ? id1 + "_" + id2 : id2 + "_" + id1;
    }

    private void sendMessage(String sender, String receiver, String message) {
        DatabaseReference ref = FirebaseHelper.getDatabase().getReference();
        long timestamp = System.currentTimeMillis();

        // 1. Lưu tin nhắn vào cuộc hội thoại
        String msgKey = ref.child("Chats").child(roomId).push().getKey();
        ChatMessage chatMessage = new ChatMessage(sender, receiver, message, timestamp, "text");
        
        Map<String, Object> messageValues = new HashMap<>();
        messageValues.put("/Chats/" + roomId + "/" + msgKey, chatMessage);

        // 2. Cập nhật danh sách Inbox cho cả 2 người
        Map<String, Object> chatListData = new HashMap<>();
        chatListData.put("id", receiver);
        chatListData.put("lastMessage", message);
        chatListData.put("timestamp", timestamp);
        messageValues.put("/ChatList/" + sender + "/" + receiver, chatListData);

        Map<String, Object> chatListDataReceiver = new HashMap<>();
        chatListDataReceiver.put("id", sender);
        chatListDataReceiver.put("lastMessage", message);
        chatListDataReceiver.put("timestamp", timestamp);
        messageValues.put("/ChatList/" + receiver + "/" + sender, chatListDataReceiver);

        // Thực hiện cập nhật đồng loạt (Atomic update)
        ref.updateChildren(messageValues).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Gửi tin nhắn thất bại", task.getException());
                Toast.makeText(ChatActivity.this, "Không thể gửi tin nhắn. Kiểm tra kết nối!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void readMessages() {
        reference = FirebaseHelper.getDatabase().getReference("Chats").child(roomId);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mChat.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    ChatMessage chat = dataSnapshot.getValue(ChatMessage.class);
                    if (chat != null) {
                        mChat.add(chat);
                    }
                }
                messageAdapter.notifyDataSetChanged();
                if (messageAdapter.getItemCount() > 0) {
                    rvChat.scrollToPosition(messageAdapter.getItemCount() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Lỗi đọc tin nhắn: " + error.getMessage());
            }
        });
    }
}
