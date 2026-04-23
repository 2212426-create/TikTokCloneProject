package com.example.tiktokcloneproject.adapters;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.activity.CommentActivity;
import com.example.tiktokcloneproject.activity.DeleteVideoSettingActivity;
import com.example.tiktokcloneproject.activity.ProfileActivity;
import com.example.tiktokcloneproject.helper.GlobalVariable;
import com.example.tiktokcloneproject.helper.OnSwipeTouchListener;
import com.example.tiktokcloneproject.model.Notification;
import com.example.tiktokcloneproject.model.Video;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.firebase.auth.FirebaseAuth;
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
    private int currentPosition = 0;
    private final Map<Integer, VideoViewHolder> activeHolders = new HashMap<>();

    public VideoAdapter(Context context, List<Video> videos) {
        this.context = context;
        this.videos = videos;
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
        if (holder.getBindingAdapterPosition() == currentPosition) {
            holder.playVideo();
        }
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

    public static void setUser(FirebaseUser user) {
        // This method is called from Activities/Fragments to set the current user.
        // The VideoViewHolder already uses FirebaseAuth.getInstance().getCurrentUser().
    }

    public void updateWatchCount(int position) {
        if (videos != null && position >= 0 && position < videos.size()) {
            String videoId = videos.get(position).getVideoId();
            FirebaseFirestore.getInstance().collection("videos").document(videoId)
                    .update("watchCount", FieldValue.increment(1));
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
        
        String authorId, videoId, currentUserId;
        int totalLikes, totalComments;
        boolean isLiked = false, isPlaying = false;
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        ListenerRegistration videoListener;

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
                LoadControl loadControl = new DefaultLoadControl.Builder()
                        .setBufferDurationsMs(500, 2000, 500, 500)
                        .setPrioritizeTimeOverSizeThresholds(true)
                        .build();

                DataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                        .setAllowCrossProtocolRedirects(true)
                        .setConnectTimeoutMs(5000)
                        .setReadTimeoutMs(5000);

                DataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory()
                        .setCache(GlobalVariable.getVideoCache())
                        .setUpstreamDataSourceFactory(httpDataSourceFactory)
                        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

                exoPlayer = new ExoPlayer.Builder(itemView.getContext())
                        .setLoadControl(loadControl)
                        .setMediaSourceFactory(new DefaultMediaSourceFactory(cacheDataSourceFactory))
                        .build();
                
                videoView.setPlayer(exoPlayer);
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
                
                exoPlayer.addListener(new Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int state) {
                        if (pbLoading == null) return;
                        pbLoading.setVisibility(state == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
                    }
                });
            }
        }

        public void playVideo() {
            if (exoPlayer == null) return;
            exoPlayer.play();
            isPlaying = true;
            if (imvAppear != null) imvAppear.setVisibility(View.GONE);
            if (imvPause != null) imvPause.setVisibility(View.GONE);
        }

        public void pauseVideo() {
            if (exoPlayer != null) {
                exoPlayer.pause();
                isPlaying = false;
                if (imvPause != null) imvPause.setVisibility(View.VISIBLE);
            }
        }

        public void cleanup() {
            if (videoListener != null) { videoListener.remove(); videoListener = null; }
            if (exoPlayer != null) {
                exoPlayer.release();
                exoPlayer = null;
                videoView.setPlayer(null);
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        public void setVideoObjects(Video video, int position) {
            if (video == null) return;
            
            this.authorId = video.getAuthorId();
            this.videoId = video.getVideoId();
            this.totalLikes = video.getTotalLikes();
            this.totalComments = video.getTotalComments();
            this.currentUserId = (currentUser != null) ? currentUser.getUid() : "";

            tvTitle.setText("@" + (video.getUsername() != null ? video.getUsername() : "user"));
            txvDescription.setText(video.getDescription());
            tvComment.setText(String.valueOf(totalComments));
            tvFavorites.setText(String.valueOf(totalLikes));

            if (video.getVideoUri() != null) {
                initPlayer();
                exoPlayer.setMediaItem(MediaItem.fromUri(video.getVideoUri()));
                exoPlayer.prepare();
                exoPlayer.setPlayWhenReady(position == currentPosition);
            }

            StorageReference ref = FirebaseStorage.getInstance().getReference().child("/user_avatars").child(authorId);
            Glide.with(itemView.getContext()).load(ref).placeholder(R.drawable.splash_background).circleCrop().into(imvAvatar);

            if (videoListener != null) videoListener.remove();
            videoListener = db.collection("videos").document(videoId).addSnapshotListener((doc, e) -> {
                if (e != null || doc == null || !doc.exists()) return;
                totalLikes = doc.getLong("totalLikes") != null ? doc.getLong("totalLikes").intValue() : 0;
                totalComments = doc.getLong("totalComments") != null ? doc.getLong("totalComments").intValue() : 0;
                tvFavorites.setText(String.valueOf(totalLikes));
                tvComment.setText(String.valueOf(totalComments));
            });

            db.collection("likes").document(currentUserId + "_" + videoId).addSnapshotListener((doc, e) -> {
                isLiked = doc != null && doc.exists();
                tvFavorites.setCompoundDrawablesWithIntrinsicBounds(0, isLiked ? R.drawable.ic_fill_favorite : R.drawable.ic_favorite, 0, 0);
            });
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.videoView) {
                if (isPlaying) pauseVideo(); else playVideo();
            } else if (id == R.id.imvAvatar || id == R.id.tvTitle) {
                moveToProfile(authorId);
            } else if (id == R.id.tvComment) {
                showComments();
            } else if (id == R.id.tvFavorites) {
                toggleLike();
            } else if (id == R.id.imvVolume) {
                toggleVolume();
            } else if (id == R.id.imvShare) {
                shareVideo();
            } else if (id == R.id.imvMore) {
                showMoreOptions();
            }
        }

        private void showMoreOptions() {
            boolean isOwner = currentUserId.equals(authorId);
            String[] options = isOwner ? new String[]{"Copy Link", "Delete Video"} : new String[]{"Copy Link", "Report"};
            
            new AlertDialog.Builder(context)
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            ClipboardManager cb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            cb.setPrimaryClip(ClipData.newPlainText("Link", "video_id:" + videoId));
                            Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show();
                        } else if (which == 1) {
                            if (isOwner) openDeleteActivity(); else Toast.makeText(context, "Reported", Toast.LENGTH_SHORT).show();
                        }
                    }).show();
        }

        private void openDeleteActivity() {
            Intent intent = new Intent(context, DeleteVideoSettingActivity.class);
            intent.putExtra("videoId", videoId);
            intent.putExtra("authorId", authorId);
            context.startActivity(intent);
        }

        private void toggleLike() {
            if (currentUser == null) return;
            DocumentReference likeRef = db.collection("likes").document(currentUserId + "_" + videoId);
            DocumentReference videoRef = db.collection("videos").document(videoId);
            if (isLiked) {
                likeRef.delete();
                videoRef.update("totalLikes", FieldValue.increment(-1));
            } else {
                Map<String, Object> like = new HashMap<>();
                like.put("userId", currentUserId);
                like.put("videoId", videoId);
                likeRef.set(like);
                videoRef.update("totalLikes", FieldValue.increment(1));
                if (!currentUserId.equals(authorId)) Notification.pushNotification(currentUserId, authorId, "like");
            }
        }

        private void toggleVolume() {
            if (exoPlayer == null) return;
            boolean isMuted = exoPlayer.getVolume() == 0;
            exoPlayer.setVolume(isMuted ? 1.0f : 0f);
            imvVolume.setImageResource(isMuted ? R.drawable.ic_baseline_volume_up_24 : R.drawable.ic_baseline_volume_off_24);
        }

        private void showComments() {
            Intent intent = new Intent(context, CommentActivity.class);
            intent.putExtra("videoId", videoId);
            intent.putExtra("authorId", authorId);
            context.startActivity(intent);
        }

        private void shareVideo() {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "Watch this video!");
            context.startActivity(Intent.createChooser(intent, "Share"));
        }

        private void moveToProfile(String uid) {
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra("uid", uid);
            context.startActivity(intent);
        }
    }
}
