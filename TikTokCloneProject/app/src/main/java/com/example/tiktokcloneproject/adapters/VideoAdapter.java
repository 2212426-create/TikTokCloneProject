package com.example.tiktokcloneproject.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.activity.CommentActivity;
import com.example.tiktokcloneproject.activity.ProfileActivity;
import com.example.tiktokcloneproject.helper.GlobalVariable;
import com.example.tiktokcloneproject.helper.LegacyHashtagFixer;
import com.example.tiktokcloneproject.helper.OnSwipeTouchListener;
import com.example.tiktokcloneproject.helper.RecommendationHelper;
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
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
    private List<Video> videos;
    private Context context;
    private int currentPosition = 0;
    private final Map<Integer, VideoViewHolder> activeHolders = new HashMap<>();
    private FirebaseUser mUser;

    public VideoAdapter(Context context, List<Video> videos) {
        this.context = context;
        this.videos = videos;
    }

    public void setUser(FirebaseUser user) {
        mUser = user;
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
        if (holder.getBindingAdapterPosition() == currentPosition) holder.playVideo();
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
    public int getItemCount() { return videos != null ? videos.size() : 0; }

    public void updateCurrentPosition(int pos) { this.currentPosition = pos; }

    public int getCurrentPosition() { return currentPosition; }
    
    public void pauseVideo(int position) {
        VideoViewHolder holder = activeHolders.get(position);
        if (holder != null) holder.pauseVideo();
    }

    public void playVideo(int position) {
        VideoViewHolder holder = activeHolders.get(position);
        if (holder != null) holder.playVideo();
    }

    public void updateWatchCount(int position) {
        if (position >= 0 && position < videos.size()) {
            RecommendationHelper.recordInterest(videos.get(position).getDescription());
        }
    }

    public class VideoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        StyledPlayerView videoView;
        ExoPlayer exoPlayer;
        ImageView imvAvatar, imvLike, imvComment, imvShare;
        TextView tvComment, tvFavorites, tvTitle, txvDescription, tvHashtags;
        ProgressBar pbLoading;
        String authorId, videoId;
        boolean isLiked = false;
        Video currentVideo;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoView);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvFavorites = itemView.findViewById(R.id.tvFavorites);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            txvDescription = itemView.findViewById(R.id.txvDescription);
            tvHashtags = itemView.findViewById(R.id.tvHashtags);
            imvAvatar = itemView.findViewById(R.id.imvAvatar);
            imvLike = itemView.findViewById(R.id.imvLike);
            imvComment = itemView.findViewById(R.id.imvComment);
            imvShare = itemView.findViewById(R.id.imvShare);
            pbLoading = itemView.findViewById(R.id.pbLoading);

            videoView.setOnClickListener(this);
            imvAvatar.setOnClickListener(this);
            imvLike.setOnClickListener(this);
            imvComment.setOnClickListener(this);
            imvShare.setOnClickListener(this);

            videoView.setOnTouchListener(new OnSwipeTouchListener(itemView.getContext()) {
                @Override public void onSwipeLeft() { moveToProfile(authorId); }
            });
        }

        private void initPlayer() {
            if (exoPlayer == null) {
                LoadControl loadControl = new DefaultLoadControl.Builder().setBufferDurationsMs(500, 2000, 500, 500).build();
                DataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true);
                DataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory().setCache(GlobalVariable.getVideoCache()).setUpstreamDataSourceFactory(httpDataSourceFactory);
                exoPlayer = new ExoPlayer.Builder(itemView.getContext()).setLoadControl(loadControl).setMediaSourceFactory(new DefaultMediaSourceFactory(cacheDataSourceFactory)).build();
                videoView.setPlayer(exoPlayer);
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
                exoPlayer.addListener(new Player.Listener() {
                    @Override public void onPlaybackStateChanged(int state) {
                        if (pbLoading != null) pbLoading.setVisibility(state == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
                    }
                });
            }
        }

        public void playVideo() { if (exoPlayer != null) exoPlayer.play(); }
        public void pauseVideo() { if (exoPlayer != null) exoPlayer.pause(); }

        public void cleanup() {
            if (exoPlayer != null) { exoPlayer.release(); exoPlayer = null; videoView.setPlayer(null); }
        }

        private void updateLikeUI(boolean liked) {
            imvLike.setImageResource(R.drawable.ic_favorite);
            if (liked) {
                imvLike.setColorFilter(Color.parseColor("#FE2C55")); // Màu hồng/đỏ TikTok khi đã thả tim
            } else {
                imvLike.setColorFilter(Color.WHITE); // Màu trắng khi chưa thả tim
            }
        }

        public void setVideoObjects(Video video, int position) {
            if (video == null) return;
            this.currentVideo = video;
            this.authorId = video.getAuthorId();
            this.videoId = video.getVideoId();
            imvAvatar.setImageResource(R.drawable.default_avatar);

            if (video.getVideoUri() != null) {
                initPlayer();
                exoPlayer.setMediaItem(MediaItem.fromUri(video.getVideoUri()));
                exoPlayer.prepare();
                exoPlayer.setPlayWhenReady(position == currentPosition);
            }

            // 1. Tên người dùng (@username) và Avatar - Tải động từ profiles
            String username = video.getUsername();
            tvTitle.setText(username != null && !username.isEmpty() ? "@" + username : "@User");
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("profiles").document(authorId)
                    .get().addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String realUsername = doc.getString("username");
                            String avatarUrl = doc.getString("avatarUrl");
                            if (realUsername != null && !realUsername.isEmpty()) {
                                tvTitle.setText("@" + realUsername);
                            }
                            if (avatarUrl != null && !avatarUrl.isEmpty() && context != null) {
                                com.bumptech.glide.Glide.with(context)
                                        .load(avatarUrl)
                                        .placeholder(R.drawable.default_avatar)
                                        .circleCrop()
                                        .into(imvAvatar);
                            }
                        }
                    });

            // 2. Xử lý Mô tả và Hashtag
            String description = video.getDescription();
            StringBuilder hashtagsStr = new StringBuilder();
            String cleanDescription = description != null ? description : "";
            
            // Regex lấy hashtag (bao gồm tiếng Việt)
            final String REGEX = "#([A-Za-z0-9_\\u00C0-\\u1EF9-]+)";
            
            // Lấy từ mảng hashtags nếu có
            List<String> hashtagsList = video.getHashtags();
            if (hashtagsList != null && !hashtagsList.isEmpty()) {
                for (String tag : hashtagsList) {
                    if (!tag.startsWith("#")) hashtagsStr.append("#");
                    hashtagsStr.append(tag).append(" ");
                }
            } else if (description != null && description.contains("#")) {
                Matcher matcher = Pattern.compile(REGEX).matcher(description);
                while (matcher.find()) {
                    hashtagsStr.append(matcher.group(0)).append(" ");
                }
            }

            // Hiển thị Hashtag (Vị trí: Ngay dưới Username nhờ XML)
            if (hashtagsStr.length() > 0) {
                tvHashtags.setText(hashtagsStr.toString().trim());
                tvHashtags.setTextColor(Color.parseColor("#3D85C6")); // Xanh TikTok
                tvHashtags.setVisibility(View.VISIBLE);
                
                // Loại bỏ hashtag khỏi mô tả để hiển thị sạch hơn bên dưới
                cleanDescription = cleanDescription.replaceAll(REGEX, "").trim();
            } else {
                // Ẩn view hashtag nếu video không có hashtag. 
                // Đồng thời huỷ bỏ tự động gọi AI khi lướt feed để tránh cạn kiệt Quota API (Lỗi 429).
                tvHashtags.setVisibility(View.GONE);
            }

            // Hiển thị Mô tả (nte / khinh khí cầu) - Nằm dưới Hashtag
            txvDescription.setText(cleanDescription.isEmpty() ? (description != null ? description : "") : cleanDescription);

            // 3. REAL-TIME DATA (Likes and Comments)
            com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();
            firestore.collection("likes").document(videoId).addSnapshotListener((snapshot, error) -> {
                if (snapshot != null && snapshot.exists()) {
                    Map<String, Object> data = snapshot.getData();
                    int likesCount = data != null ? data.size() : 0;
                    tvFavorites.setText(String.valueOf(likesCount));
                    
                    String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ? 
                                        com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
                    isLiked = !currentUid.isEmpty() && data != null && data.containsKey(currentUid);
                    updateLikeUI(isLiked);
                } else {
                    tvFavorites.setText("0");
                    isLiked = false;
                    updateLikeUI(false);
                }
            });

            firestore.collection("comments").whereEqualTo("videoId", videoId).addSnapshotListener((snapshots, error) -> {
                if (snapshots != null) {
                    tvComment.setText(String.valueOf(snapshots.size()));
                } else {
                    tvComment.setText("0");
                }
            });
        }

        @Override public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.videoView) {
                if (exoPlayer != null && exoPlayer.isPlaying()) pauseVideo();
                else playVideo();
            } else if (id == R.id.imvLike) {
                toggleLikeLocal();
            } else if (id == R.id.imvComment) {
                showComments();
            } else if (id == R.id.imvAvatar) {
                moveToProfile(authorId);
            } else if (id == R.id.imvShare) {
                shareDemo();
            }
        }

        private void toggleLikeLocal() {
            String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ? 
                                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
            if (currentUid.isEmpty()) {
                Toast.makeText(context, "Vui lòng đăng nhập để thả tim!", Toast.LENGTH_SHORT).show();
                return;
            }

            isLiked = !isLiked;
            updateLikeUI(isLiked);

            com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();
            com.google.firebase.firestore.DocumentReference likeDocRef = firestore.collection("likes").document(videoId);
            
            if (isLiked) {
                Map<String, Object> likeData = new HashMap<>();
                likeData.put(currentUid, true);
                likeDocRef.set(likeData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        updateFirestoreTotalLikes(firestore);
                    })
                    .addOnFailureListener(e -> {
                        isLiked = false;
                        updateLikeUI(false);
                        Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                
                if (currentVideo != null) {
                    RecommendationHelper.recordInterest(currentVideo.getDescription());
                }
            } else {
                Map<String, Object> updates = new HashMap<>();
                updates.put(currentUid, com.google.firebase.firestore.FieldValue.delete());
                likeDocRef.update(updates)
                    .addOnSuccessListener(aVoid -> {
                        updateFirestoreTotalLikes(firestore);
                    })
                    .addOnFailureListener(e -> {
                        isLiked = true;
                        updateLikeUI(true);
                        Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            }
        }

        private void updateFirestoreTotalLikes(com.google.firebase.firestore.FirebaseFirestore firestore) {
            firestore.collection("likes").document(videoId).get().addOnSuccessListener(snapshot -> {
                if (snapshot != null && snapshot.exists()) {
                    int newLikesCount = snapshot.getData() != null ? snapshot.getData().size() : 0;
                    firestore.collection("videos").document(videoId).update("totalLikes", newLikesCount);
                }
            });
        }

        private void shareDemo() {
            Toast.makeText(context, "Đang mở bảng chia sẻ...", Toast.LENGTH_SHORT).show();
        }

        private void showComments() {
            Intent intent = new Intent(context, CommentActivity.class);
            intent.putExtra("videoId", videoId);
            context.startActivity(intent);
        }

        private void moveToProfile(String uid) { 
            Intent intent = new Intent(context, ProfileActivity.class); 
            intent.putExtra("id", uid);
            context.startActivity(intent); 
        }
    }
}
