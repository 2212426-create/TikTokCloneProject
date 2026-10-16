package com.example.tiktokcloneproject.adapters;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.model.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;

    private Context mContext;
    private List<ChatMessage> mChat;
    private String imageUrl; // URL ảnh của đối phương

    public ChatAdapter(Context mContext, List<ChatMessage> mChat) {
        this.mContext = mContext;
        this.mChat = mChat;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_chat_right, parent, false);
            return new ViewHolder(view);
        } else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_chat_left, parent, false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage chat = mChat.get(position);
        String message = chat.getMessage();

        if (message.contains("[APPEAL:") && message.endsWith("]")) {
            setupAppealMessage(holder.show_message, message);
        } else {
            setupMessageText(holder.show_message, message);
        }

        // Hiện ảnh đại diện cho mọi tin nhắn bên trái (Người nhận)
        if (getItemViewType(position) == MSG_TYPE_LEFT && holder.profile_image != null) {
            holder.profile_image.setVisibility(View.VISIBLE);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(mContext)
                    .load(imageUrl)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(holder.profile_image);
            } else {
                holder.profile_image.setImageResource(R.drawable.default_avatar);
            }
        }
    }

    private void setupAppealMessage(TextView textView, String fullMessage) {
        int startIndex = fullMessage.indexOf("nhấn vào đây");
        if (startIndex == -1) {
            textView.setText(fullMessage);
            return;
        }
        
        int endIndex = startIndex + "nhấn vào đây".length();
        SpannableString ss = new SpannableString(fullMessage.substring(0, fullMessage.indexOf(" [APPEAL:")));
        
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                String reportId = fullMessage.substring(fullMessage.indexOf(":") + 1, fullMessage.length() - 1);
                showAppealDialog(reportId);
            }
        };

        ss.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(ss);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setupMessageText(TextView textView, String message) {
        if (message == null) {
            textView.setText("");
            return;
        }

        // Link mới và link toptop-share-page cũ (tin nhắn đã gửi trước khi đổi tên trang share).
        String sharePrefix = "https://hubertphung.github.io/vifarm-share-page/?id=";
        int startIndex = message.indexOf(sharePrefix);
        if (startIndex == -1) {
            sharePrefix = "https://hubertphung.github.io/toptop-share-page/?id=";
            startIndex = message.indexOf(sharePrefix);
        }
        if (startIndex != -1) {
            int urlEndIndex = message.indexOf(" ", startIndex);
            if (urlEndIndex == -1) {
                urlEndIndex = message.length();
            }
            String videoId = message.substring(startIndex + sharePrefix.length(), urlEndIndex).trim();
            
            SpannableString ss = new SpannableString(message);
            
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Intent intent = new Intent(mContext, com.example.tiktokcloneproject.activity.VideoActivity.class);
                    intent.putExtra("videoId", videoId);
                    mContext.startActivity(intent);
                }
            };
            
            ss.setSpan(clickableSpan, startIndex, urlEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(ss);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            textView.setText(message);
            if (Linkify.addLinks(textView, Linkify.WEB_URLS)) {
                textView.setMovementMethod(LinkMovementMethod.getInstance());
            } else {
                textView.setMovementMethod(null);
            }
        }
    }

    private void showAppealDialog(String reportId) {
        EditText etAppeal = new EditText(mContext);
        etAppeal.setHint("Nhập nội dung phản ánh của bạn...");
        
        new AlertDialog.Builder(mContext)
                .setTitle("Phản ánh báo cáo")
                .setView(etAppeal)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String content = etAppeal.getText().toString().trim();
                    if (!content.isEmpty()) {
                        submitAppeal(reportId, content);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void submitAppeal(String reportId, String content) {
        FirebaseFirestore.getInstance().collection("reports").document(reportId)
                .update("appeal", content)
                .addOnSuccessListener(aVoid -> Toast.makeText(mContext, "Đã gửi phản ánh thành công!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(mContext, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return mChat.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mChat.get(position).getSenderId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView show_message;
        public CircleImageView profile_image;

        public ViewHolder(View itemView) {
            super(itemView);
            show_message = itemView.findViewById(R.id.tvMessageLeft);
            if (show_message == null) {
                show_message = itemView.findViewById(R.id.tvMessageRight);
            }
            profile_image = itemView.findViewById(R.id.ivUserChatLeft);
        }
    }
}
