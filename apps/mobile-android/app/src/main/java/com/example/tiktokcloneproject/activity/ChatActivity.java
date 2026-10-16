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
    private String receiverAvatar;
    private String roomId;
    private String currentUsername = "";
    private final String TAG = "ChatActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        receiverId = getIntent().getStringExtra("receiver_id");
        receiverName = getIntent().getStringExtra("receiver_name");
        receiverAvatar = getIntent().getStringExtra("receiver_avatar");

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

        // Khởi tạo Adapter
        mChat = new ArrayList<>();
        messageAdapter = new ChatAdapter(ChatActivity.this, mChat);
        rvChat.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(linearLayoutManager);
        rvChat.setAdapter(messageAdapter);

        // Hiển thị ảnh đại diện ngay nếu đã có link từ Intent
        if (receiverAvatar != null && !receiverAvatar.isEmpty()) {
            Glide.with(this).load(receiverAvatar).placeholder(R.drawable.default_avatar).into(ivUserChat);
            messageAdapter.setImageUrl(receiverAvatar);
        }

        // Tải lại thông tin mới nhất từ Firestore để đảm bảo ảnh luôn đúng
        FirebaseFirestore.getInstance().collection("profiles").document(receiverId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String avatarUrl = doc.getString("avatarUrl");
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(this).load(avatarUrl).placeholder(R.drawable.default_avatar).into(ivUserChat);
                            if (messageAdapter != null) {
                                messageAdapter.setImageUrl(avatarUrl);
                                messageAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });

        if (fUser != null) {
            FirebaseFirestore.getInstance().collection("profiles").document(fUser.getUid())
                    .get().addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            currentUsername = doc.getString("username");
                        }
                    });
        }

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

        String msgKey = ref.child("Chats").child(roomId).push().getKey();
        ChatMessage chatMessage = new ChatMessage(sender, receiver, message, timestamp, "text");
        
        Map<String, Object> messageValues = new HashMap<>();
        messageValues.put("/Chats/" + roomId + "/" + msgKey, chatMessage);

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

        ref.updateChildren(messageValues).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (currentUsername != null && !currentUsername.isEmpty()) {
                    com.example.tiktokcloneproject.model.Notification.pushNotification(
                        currentUsername, 
                        receiver, 
                        com.example.tiktokcloneproject.helper.StaticVariable.CHAT
                    );
                } else {
                    FirebaseFirestore.getInstance().collection("profiles").document(sender)
                            .get().addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    String name = doc.getString("username");
                                    com.example.tiktokcloneproject.model.Notification.pushNotification(
                                        name != null ? name : "Ai đó", 
                                        receiver, 
                                        com.example.tiktokcloneproject.helper.StaticVariable.CHAT
                                    );
                                }
                            });
                }
            } else {
                Log.e(TAG, "Gửi tin nhắn thất bại", task.getException());
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
