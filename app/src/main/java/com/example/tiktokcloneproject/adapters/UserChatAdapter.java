package com.example.tiktokcloneproject.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.activity.ChatActivity;
import com.example.tiktokcloneproject.activity.ProfileActivity;
import com.example.tiktokcloneproject.helper.FirebaseHelper;
import com.example.tiktokcloneproject.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserChatAdapter extends RecyclerView.Adapter<UserChatAdapter.ViewHolder> {

    private Context mContext;
    private List<User> mUsers;

    public UserChatAdapter(Context mContext, List<User> mUsers) {
        this.mContext = mContext;
        this.mUsers = mUsers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_chat_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = mUsers.get(position);
        
        holder.username.setText(user.getUsername() != null ? user.getUsername() : "User");
        
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(mContext).load(user.getAvatarUrl()).placeholder(R.drawable.default_avatar).into(holder.profile_image);
        } else {
            holder.profile_image.setImageResource(R.drawable.default_avatar);
        }

        lastMessage(user.getUserId(), holder.last_msg);

        holder.profile_image.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, ProfileActivity.class);
            intent.putExtra("id", user.getUserId());
            mContext.startActivity(intent);
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, ChatActivity.class);
            intent.putExtra("receiver_id", user.getUserId());
            intent.putExtra("receiver_name", user.getUsername());
            mContext.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView username;
        public CircleImageView profile_image;
        public TextView last_msg;

        public ViewHolder(View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.tvUserNameChatList);
            profile_image = itemView.findViewById(R.id.ivUserChatList);
            last_msg = itemView.findViewById(R.id.tvLastMessageChatList);
        }
    }

    private void lastMessage(String userid, TextView last_msg) {
        final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        DatabaseReference reference = FirebaseHelper.getDatabase().getReference("ChatList")
                .child(firebaseUser.getUid()).child(userid);
                
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String lastMsg = snapshot.child("lastMessage").getValue(String.class);
                    if (lastMsg != null) {
                        last_msg.setText(lastMsg);
                    } else {
                        last_msg.setText("No message");
                    }
                } else {
                    last_msg.setText("No message");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
