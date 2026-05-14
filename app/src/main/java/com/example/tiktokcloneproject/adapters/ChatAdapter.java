package com.example.tiktokcloneproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.model.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;

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

    // Thêm constructor để nhận URL ảnh
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
        holder.show_message.setText(chat.getMessage());

        // Hiển thị ảnh đại diện cho tin nhắn bên trái (của người kia)
        if (getItemViewType(position) == MSG_TYPE_LEFT && holder.profile_image != null) {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(mContext).load(imageUrl).placeholder(R.drawable.default_avatar).into(holder.profile_image);
            } else {
                holder.profile_image.setImageResource(R.drawable.default_avatar);
            }
        }
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
