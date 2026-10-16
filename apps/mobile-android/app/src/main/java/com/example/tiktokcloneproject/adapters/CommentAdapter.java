package com.example.tiktokcloneproject.adapters;

import android.content.Context;
import android.graphics.Color;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentAdapter extends ArrayAdapter<Comment> {
    private Context context;
    private int resource;
    private List<Comment> objects;
    private String currentUid;
    private FirebaseFirestore db;
    private Map<String, Boolean> likedMap = new HashMap<>(); // Cache user's like status

    public CommentAdapter(@NonNull Context context, int resource, @NonNull List<Comment> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.objects = objects;
        this.currentUid = FirebaseAuth.getInstance().getUid();
        this.db = FirebaseFirestore.getInstance();
    }

    static class ViewHolder {
        ImageView imvAvatar;
        TextView tvUsername;
        TextView tvContent;
        ImageView imvLike;
        TextView tvTotalLikes;
        TextView tvReplyButton;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(resource, parent, false);
            holder = new ViewHolder();
            holder.imvAvatar = convertView.findViewById(R.id.imvAvatarInComment);
            holder.tvUsername = convertView.findViewById(R.id.txvUsernameInComment);
            holder.tvContent = convertView.findViewById(R.id.txvComment);
            holder.imvLike = convertView.findViewById(R.id.imvLikeInComment);
            holder.tvTotalLikes = convertView.findViewById(R.id.txvTotalLikeComment);
            holder.tvReplyButton = convertView.findViewById(R.id.txvReplyButton);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Comment comment = objects.get(position);
        if (comment == null) return convertView;
        
        String commentId = comment.getCommentId();

        // 0. Handle dummy expand/collapse rows
        if (commentId != null && (commentId.endsWith("_expand") || commentId.endsWith("_collapse"))) {
            holder.imvAvatar.setVisibility(View.GONE);
            holder.tvUsername.setVisibility(View.GONE);
            holder.imvLike.setVisibility(View.GONE);
            holder.tvTotalLikes.setVisibility(View.GONE);
            if (holder.tvReplyButton != null) holder.tvReplyButton.setVisibility(View.GONE);
            
            int padding48 = (int) (48 * context.getResources().getDisplayMetrics().density);
            int padding8 = (int) (8 * context.getResources().getDisplayMetrics().density);
            convertView.setPadding(padding48, padding8, padding8, padding8);
            
            if (commentId.endsWith("_expand")) {
                int remaining = comment.getTotalReplies() - 2;
                holder.tvContent.setText("— Xem thêm " + remaining + " câu trả lời...");
                holder.tvContent.setTextColor(Color.parseColor("#3D85C6"));
                holder.tvContent.setOnClickListener(v -> {
                    if (context instanceof com.example.tiktokcloneproject.activity.CommentActivity) {
                        ((com.example.tiktokcloneproject.activity.CommentActivity) context).expandComment(comment.getParentId());
                    }
                });
            } else {
                holder.tvContent.setText("— Thu gọn câu trả lời");
                holder.tvContent.setTextColor(Color.parseColor("#3D85C6"));
                holder.tvContent.setOnClickListener(v -> {
                    if (context instanceof com.example.tiktokcloneproject.activity.CommentActivity) {
                        ((com.example.tiktokcloneproject.activity.CommentActivity) context).collapseComment(comment.getParentId());
                    }
                });
            }
            
            convertView.setOnClickListener(v -> {
                if (commentId.endsWith("_expand")) {
                    if (context instanceof com.example.tiktokcloneproject.activity.CommentActivity) {
                        ((com.example.tiktokcloneproject.activity.CommentActivity) context).expandComment(comment.getParentId());
                    }
                } else {
                    if (context instanceof com.example.tiktokcloneproject.activity.CommentActivity) {
                        ((com.example.tiktokcloneproject.activity.CommentActivity) context).collapseComment(comment.getParentId());
                    }
                }
            });
            return convertView;
        }

        // Restore default visibilities for recycled views
        holder.imvAvatar.setVisibility(View.VISIBLE);
        holder.tvUsername.setVisibility(View.VISIBLE);
        holder.imvLike.setVisibility(View.VISIBLE);
        holder.tvTotalLikes.setVisibility(View.VISIBLE);
        if (holder.tvReplyButton != null) holder.tvReplyButton.setVisibility(View.VISIBLE);
        holder.tvContent.setTextColor(context.getResources().getColor(R.color.dark_text_primary));
        holder.tvContent.setOnClickListener(null);
        convertView.setOnClickListener(null);

        // 1. Init UI with data from model
        if (comment.getParentId() != null && !comment.getParentId().isEmpty() && comment.getParentUsername() != null) {
            String text = "@" + comment.getParentUsername() + ": " + comment.getContent();
            android.text.SpannableString ss = new android.text.SpannableString(text);
            int start = 0; 
            int end = comment.getParentUsername().length() + 1; 
            ss.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor("#3D85C6")), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.tvContent.setText(ss);
            
            int padding8 = (int) (8 * context.getResources().getDisplayMetrics().density);
            int padding48 = (int) (48 * context.getResources().getDisplayMetrics().density);
            convertView.setPadding(padding48, padding8, padding8, padding8);
            
            ViewGroup.LayoutParams params = holder.imvAvatar.getLayoutParams();
            params.width = (int) (28 * context.getResources().getDisplayMetrics().density);
            params.height = (int) (28 * context.getResources().getDisplayMetrics().density);
            holder.imvAvatar.setLayoutParams(params);
        } else {
            holder.tvContent.setText(comment.getContent());
            
            int padding8 = (int) (8 * context.getResources().getDisplayMetrics().density);
            convertView.setPadding(padding8, padding8, padding8, padding8);
            
            ViewGroup.LayoutParams params = holder.imvAvatar.getLayoutParams();
            params.width = (int) (40 * context.getResources().getDisplayMetrics().density);
            params.height = (int) (40 * context.getResources().getDisplayMetrics().density);
            holder.imvAvatar.setLayoutParams(params);
        }
        holder.tvTotalLikes.setText(String.valueOf(comment.getTotalLikes()));
        holder.imvAvatar.setImageResource(R.drawable.default_avatar);
        holder.tvUsername.setText("");
        
        // Tag used to identify view during async loading
        final String currentCommentId = commentId;
        holder.imvLike.setTag(currentCommentId);

        // 2. Load User Profile (Username & Avatar)
        db.collection("profiles").document(comment.getAuthorId())
                .get().addOnSuccessListener(doc -> {
                    if (doc.exists() && currentCommentId.equals(holder.imvLike.getTag())) {
                        holder.tvUsername.setText(doc.getString("username"));
                        String avatarUrl = doc.getString("avatarUrl");
                        if (avatarUrl != null && context != null) {
                            Glide.with(context)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.default_avatar)
                                    .circleCrop()
                                    .into(holder.imvAvatar);
                        }
                    }
                });

        // Disable avatar click - do not navigate to profile
        holder.imvAvatar.setClickable(false);
        holder.imvAvatar.setFocusable(false);
        holder.imvAvatar.setOnClickListener(null);

        // 3. Handle Like Status (TikTok Style Toggle)
        boolean isLikedLocally = likedMap.getOrDefault(commentId, false);
        updateLikeUI(holder.imvLike, isLikedLocally);
        
        // If not in cache, fetch from Firestore
        if (currentUid != null && !likedMap.containsKey(commentId)) {
            db.collection("comment_likes").document(commentId)
                    .get().addOnSuccessListener(doc -> {
                        boolean isLiked = doc.exists() && doc.contains(currentUid);
                        likedMap.put(commentId, isLiked);
                        // Check tag again because view might be recycled
                        if (currentCommentId.equals(holder.imvLike.getTag())) {
                            updateLikeUI(holder.imvLike, isLiked);
                        }
                    });
        }

        // 4. Like Click Listener
        holder.imvLike.setOnClickListener(v -> {
            if (currentUid == null) return;

            boolean wasLiked = likedMap.getOrDefault(commentId, false);
            boolean isLikedNow = !wasLiked;
            
            // --- Instant Local Update ---
            likedMap.put(commentId, isLikedNow);
            int newTotal = comment.getTotalLikes() + (isLikedNow ? 1 : -1);
            comment.setTotalLikes(newTotal);
            
            updateLikeUI(holder.imvLike, isLikedNow);
            holder.tvTotalLikes.setText(String.valueOf(newTotal));

            // --- Async Server Update ---
            DocumentReference likeRef = db.collection("comment_likes").document(commentId);
            DocumentReference commentRef = db.collection("comments").document(commentId);

            if (isLikedNow) {
                Map<String, Object> data = new HashMap<>();
                data.put(currentUid, true);
                likeRef.set(data, SetOptions.merge());
                commentRef.update("totalLikes", FieldValue.increment(1));
            } else {
                Map<String, Object> updates = new HashMap<>();
                updates.put(currentUid, FieldValue.delete());
                likeRef.update(updates);
                commentRef.update("totalLikes", FieldValue.increment(-1));
            }
        });

        // 5. Reply Click Listener
        if (holder.tvReplyButton != null) {
            holder.tvReplyButton.setOnClickListener(v -> {
                if (context instanceof com.example.tiktokcloneproject.activity.CommentActivity) {
                    ((com.example.tiktokcloneproject.activity.CommentActivity) context).startReply(comment, holder.tvUsername.getText().toString());
                }
            });
        }

        return convertView;
    }

    private void updateLikeUI(ImageView imv, boolean isLiked) {
        if (isLiked) {
            imv.setImageResource(R.drawable.ic_fill_favorite);
            imv.setColorFilter(Color.parseColor("#FE2C55")); // TikTok Red
        } else {
            imv.setImageResource(R.drawable.ic_like);
            imv.clearColorFilter();
        }
    }
}
