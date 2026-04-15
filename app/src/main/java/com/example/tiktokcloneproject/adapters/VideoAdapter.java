package com.example.tiktokcloneproject.adapters;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.activity.CommentActivity;
import com.example.tiktokcloneproject.activity.DeleteVideoSettingActivity;
import com.example.tiktokcloneproject.activity.MainActivity;
import com.example.tiktokcloneproject.activity.ProfileActivity;
import com.example.tiktokcloneproject.helper.OnSwipeTouchListener;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.example.tiktokcloneproject.model.Notification;
import com.example.tiktokcloneproject.model.Video;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private List<Video> videos;
    private Context context;
    private static FirebaseUser user = null;
    private int currentPosition = 0;
    private final Map<Integer, VideoViewHolder> activeHolders = new HashMap<>();

    public VideoAdapter(Context context, List<Video> videos) {
        this.context = context;
        this.videos = videos;
    }

    public static void setUser(FirebaseUser user) {
        VideoAdapter.user = user;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VideoViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.video_container, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        holder.setVideoObjects(videos.get(position), position);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull VideoViewHolder holder) {
        activeHolders.put(holder.getBindingAdapterPosition(), holder);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull VideoViewHolder holder) {
        activeHolders.remove(holder.getBindingAdapterPosition());
        holder.pauseVideo();
    }

    @Override
    public void onViewRecycled(@NonNull VideoViewHolder holder) {
        super.onViewRecycled(holder);
        holder.cleanup();
    }

    @Override
    public int getItemCount() {
        return videos != null ? videos.size() : 0;
    }

    public void updateCurrentPosition(int pos) {
        this.currentPosition = pos;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void updateWatchCount(int position) {
        if (position >= 0 && position < videos.size()) {
            Video video = videos.get(position);
            if (video != null && video.getVideoId() != null) {
                FirebaseFirestore.getInstance().collection("videos").document(video.getVideoId())
                        .update("watchCount", FieldValue.increment(1));
            }
        }
    }

    public void pauseVideo(int position) {
        VideoViewHolder holder = activeHolders.get(position);
        if (holder != null) holder.pauseVideo();
    }

    public void playVideo(int position) {
        VideoViewHolder holder = activeHolders.get(position);
        if (holder != null) holder.playVideo();
    }

    public class VideoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        StyledPlayerView videoView;
        ExoPlayer exoPlayer;
        ImageView imvAvatar, imvPause, imvMore, imvAppear, imvVolume, imvShare;
        TextView txvDescription, tvTitle, tvComment, tvFavorites;
        ProgressBar pbLoading;
        
        String authorId, videoId, userId;
        int totalLikes, totalComments;
        boolean isLiked = false, isPlaying = false;
        float lastVolume = 1.0f;
        int clickCount = 0;
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        ListenerRegistration videoListener, profileListener;
        Handler handler = new Handler(Looper.getMainLooper());

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            txvDescription = itemView.findViewById(R.id.txvDescription);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvFavorites = itemView.findViewById(R.id.tvFavorites);
            imvAvatar = itemView.findViewById(R.id.imvAvatar);
            imvPause = itemView.findViewById(R.id.imvPause);
            imvMore = itemView.findViewById(R.id.imvMore);
            imvAppear = itemView.findViewById(R.id.imv_appear);
            imvVolume = itemView.findViewById(R.id.imvVolume);
            imvShare = itemView.findViewById(R.id.imvShare);
            pbLoading = itemView.findViewById(R.id.pbLoading);

            initPlayer();

            videoView.setOnClickListener(this);
            imvAvatar.setOnClickListener(this);
            tvTitle.setOnClickListener(this);
            tvComment.setOnClickListener(this);
            imvMore.setOnClickListener(this);
            tvFavorites.setOnClickListener(this);
            imvVolume.setOnClickListener(this);
            imvShare.setOnClickListener(this);

            videoView.setOnTouchListener(new OnSwipeTouchListener(itemView.getContext()) {
                @Override public void onSwipeLeft() { moveToProfile(authorId); }
            });
        }

        private void initPlayer() {
            if (exoPlayer == null) {
                exoPlayer = new ExoPlayer.Builder(itemView.getContext()).build();
                videoView.setPlayer(exoPlayer);
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
                
                exoPlayer.addListener(new Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int state) {
                        if (pbLoading == null) return;
                        if (state == Player.STATE_BUFFERING) {
                            pbLoading.setVisibility(View.VISIBLE);
                        } else if (state == Player.STATE_READY || state == Player.STATE_ENDED) {
                            pbLoading.setVisibility(View.GONE);
                        }
                    }
                });
            }
        }

        public void playVideo() {
            if (exoPlayer != null) {
                exoPlayer.play();
                isPlaying = true;
                if (imvAppear != null) imvAppear.setVisibility(View.GONE);
                if (imvPause != null) imvPause.setVisibility(View.GONE);
            }
        }

        public void pauseVideo() {
            if (exoPlayer != null) {
                exoPlayer.pause();
                isPlaying = false;
                if (imvPause != null) imvPause.setVisibility(View.VISIBLE);
            }
        }

        public void cleanup() {
            if (videoListener != null) {
                videoListener.remove();
                videoListener = null;
            }
            if (profileListener != null) {
                profileListener.remove();
                profileListener = null;
            }
            if (exoPlayer != null) {
                exoPlayer.stop();
                exoPlayer.release();
                exoPlayer = null;
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        public void setVideoObjects(Video video, int position) {
            if (video == null) return;
            
            // Hủy listener cũ trước khi gán dữ liệu mới
            if (videoListener != null) videoListener.remove();
            if (profileListener != null) profileListener.remove();
            
            initPlayer();

            this.authorId = video.getAuthorId();
            this.videoId = video.getVideoId();
            this.totalLikes = video.getTotalLikes();
            this.totalComments = video.getTotalComments();
            this.userId = (user != null) ? user.getUid() : "";

            if (tvTitle != null) tvTitle.setText("@" + (video.getUsername() != null ? video.getUsername() : "user"));
            if (txvDescription != null) txvDescription.setText(video.getDescription());
            if (tvComment != null) tvComment.setText(String.valueOf(totalComments));
            if (tvFavorites != null) tvFavorites.setText(String.valueOf(totalLikes));

            if (video.getVideoUri() != null) {
                MediaItem mediaItem = MediaItem.fromUri(video.getVideoUri());
                exoPlayer.setMediaItem(mediaItem);
                exoPlayer.prepare();
            }

            // Load avatar
            if (authorId != null && imvAvatar != null) {
                StorageReference ref = FirebaseStorage.getInstance().getReference().child("/user_avatars").child(authorId);
                Glide.with(itemView.getContext())
                        .load(ref)
                        .placeholder(R.drawable.splash_background)
                        .circleCrop()
                        .into(imvAvatar);
            }

            // Real-time updates với check null an toàn
            videoListener = db.collection("videos").document(videoId)
                    .addSnapshotListener((doc, e) -> {
                        if (e != null) return;
                        if (doc != null && doc.exists()) {
                            Long likes = doc.getLong("totalLikes");
                            Long comments = doc.getLong("totalComments");
                            if (likes != null && tvFavorites != null) tvFavorites.setText(String.valueOf(likes));
                            if (comments != null && tvComment != null) tvComment.setText(String.valueOf(comments));
                        }
                    });

            profileListener = db.collection("profiles").document(authorId)
                    .addSnapshotListener((doc, e) -> {
                        if (e != null) return;
                        if (doc != null && doc.exists()) {
                            String uname = doc.getString("username");
                            if (uname != null && tvTitle != null) tvTitle.setText("@" + uname);
                        }
                    });

            if (imvMore != null) imvMore.setVisibility(userId.equals(authorId) ? View.VISIBLE : View.GONE);
            checkIfLiked();
        }

        private void checkIfLiked() {
            if (userId == null || userId.isEmpty() || videoId == null) return;
            db.collection("likes").document(videoId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    isLiked = task.getResult().contains(userId);
                    updateLikeUI();
                }
            });
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.videoView) handleVideoClick();
            else if (id == R.id.imvAvatar || id == R.id.tvTitle) moveToProfile(authorId);
            else if (id == R.id.tvComment) openComments();
            else if (id == R.id.tvFavorites) handleLike();
            else if (id == R.id.imvVolume) toggleVolume();
            else if (id == R.id.imvShare) showShareDialog();
            else if (id == R.id.imvMore) handleMoreOptions();
        }

        private void handleVideoClick() {
            clickCount++;
            handler.postDelayed(() -> {
                if (clickCount == 1) {
                    if (isPlaying) {
                        pauseVideo();
                        if (imvAppear != null) {
                            imvAppear.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                            imvAppear.setVisibility(View.VISIBLE);
                        }
                    } else {
                        playVideo();
                    }
                } else if (clickCount == 2) {
                    if (!isLiked) handleLike();
                    showHeartAnimation();
                }
                clickCount = 0;
            }, 300);
        }

        private void showHeartAnimation() {
            if (imvAppear != null) {
                imvAppear.setImageResource(R.drawable.ic_fill_favorite);
                imvAppear.setVisibility(View.VISIBLE);
                handler.postDelayed(() -> imvAppear.setVisibility(View.GONE), 800);
            }
        }

        private void handleLike() {
            if (user == null) {
                showLoginDialog();
                return;
            }
            isLiked = !isLiked;
            updateLikeUI();
            DocumentReference ref = db.collection("likes").document(videoId);
            if (isLiked) {
                Map<String, Object> data = new HashMap<>();
                data.put(userId, FieldValue.serverTimestamp());
                ref.set(data, com.google.firebase.firestore.SetOptions.merge());
                db.collection("videos").document(videoId).update("totalLikes", FieldValue.increment(1));
                notifyLike();
            } else {
                ref.update(userId, FieldValue.delete());
                db.collection("videos").document(videoId).update("totalLikes", FieldValue.increment(-1));
            }
        }

        private void updateLikeUI() {
            if (tvFavorites != null) {
                tvFavorites.setCompoundDrawablesWithIntrinsicBounds(0, isLiked ? R.drawable.ic_fill_favorite : R.drawable.ic_favorite, 0, 0);
            }
        }

        private void toggleVolume() {
            if (exoPlayer == null) return;
            if (exoPlayer.getVolume() > 0) {
                lastVolume = exoPlayer.getVolume();
                exoPlayer.setVolume(0f);
                if (imvVolume != null) imvVolume.setImageResource(R.drawable.ic_baseline_volume_off_24);
            } else {
                exoPlayer.setVolume(lastVolume);
                if (imvVolume != null) imvVolume.setImageResource(R.drawable.ic_baseline_volume_up_24);
            }
        }

        private void moveToProfile(String id) {
            if (id == null) return;
            pauseVideo();
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra("id", id);
            context.startActivity(intent);
        }

        private void openComments() {
            if (user == null) { showLoginDialog(); return; }
            Intent intent = new Intent(context, CommentActivity.class);
            intent.putExtra("videoId", videoId);
            intent.putExtra("authorId", authorId);
            intent.putExtra("totalComments", totalComments);
            context.startActivity(intent);
        }

        private void handleMoreOptions() {
            Intent intent = new Intent(context, DeleteVideoSettingActivity.class);
            intent.putExtra("videoId", videoId);
            intent.putExtra("authorId", authorId);
            context.startActivity(intent);
        }

        private void showShareDialog() {
            final Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.share_video_layout);
            dialog.findViewById(R.id.btnCopyURL).setOnClickListener(v -> {
                ClipboardManager cb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                cb.setPrimaryClip(ClipData.newPlainText("link", "http://video.toptoptoptop.com/" + videoId));
                Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            dialog.findViewById(R.id.txvCancelInSharedPlace).setOnClickListener(v -> dialog.dismiss());
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                window.setGravity(Gravity.BOTTOM);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            dialog.show();
        }

        private void showLoginDialog() {
            new AlertDialog.Builder(context, R.style.AlertDialogTheme)
                    .setTitle(R.string.request_account_title)
                    .setMessage(R.string.request_account_message)
                    .setPositiveButton("Sign in", (d, w) -> context.startActivity(new Intent(context, MainActivity.class)))
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void notifyLike() {
            if (userId == null) return;
            db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String name = doc.getString("username");
                    Notification.pushNotification(name, authorId, StaticVariable.LIKE);
                }
            });
        }
    }
}
