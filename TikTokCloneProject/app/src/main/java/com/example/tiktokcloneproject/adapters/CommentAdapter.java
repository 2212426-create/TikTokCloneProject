package com.example.tiktokcloneproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.model.Comment;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class CommentAdapter extends ArrayAdapter<Comment> {
    private Context context;
    private int resource;
    private List<Comment> objects;

    public CommentAdapter(@NonNull Context context, int resource, @NonNull List<Comment> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.objects = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(resource, parent, false);
        }

        Comment comment = objects.get(position);
        ImageView imvAvatar = convertView.findViewById(R.id.imvAvatarInComment);
        TextView tvUsername = convertView.findViewById(R.id.txvUsernameInComment);
        TextView tvContent = convertView.findViewById(R.id.txvComment);

        tvContent.setText(comment.getContent());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Lấy username và avatarUrl từ profile
        db.collection("profiles").document(comment.getAuthorId())
                .get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvUsername.setText(doc.getString("username"));
                        String avatarUrl = doc.getString("avatarUrl");
                        if (avatarUrl != null) {
                            Glide.with(context)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.default_avatar)
                                    .circleCrop()
                                    .into(imvAvatar);
                        } else {
                            imvAvatar.setImageResource(R.drawable.default_avatar);
                        }
                    }
                });

        return convertView;
    }
}
