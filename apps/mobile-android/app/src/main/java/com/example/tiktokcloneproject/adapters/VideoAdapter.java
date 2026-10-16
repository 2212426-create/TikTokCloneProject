package com.example.tiktokcloneproject.adapters;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.activity.CommentActivity;
import com.example.tiktokcloneproject.activity.DescriptionVideoActivity;
import com.example.tiktokcloneproject.activity.HomeScreenActivity;
import com.example.tiktokcloneproject.activity.ProfileActivity;
import com.example.tiktokcloneproject.activity.MainActivity;
import com.example.tiktokcloneproject.helper.FirebaseHelper;
import com.example.tiktokcloneproject.helper.GlobalVariable;
import com.example.tiktokcloneproject.helper.OnSwipeTouchListener;
import com.example.tiktokcloneproject.helper.RecommendationHelper;
import com.example.tiktokcloneproject.model.ChatMessage;
import com.example.tiktokcloneproject.model.Report;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
    private static final String TAG = "VideoAdapter";
    private List<Video> videos;
    private Context context;
    private int currentPosition = 0;
    private final Set<VideoViewHolder> activeHolders = new HashSet<>();
    private FirebaseUser mUser;
    private static boolean isMuted = false;
    private ExoPlayer sharedPlayer;
    private VideoViewHolder currentPlayingHolder;
    private boolean isManuallyPaused = false;

    public VideoAdapter(Context context, List<Video> videos) {
        this.context = context;
        this.videos = videos;
    }

    private ExoPlayer getSharedPlayer(Context ctx) {
        if (sharedPlayer == null) {
            LoadControl loadControl = new DefaultLoadControl.Builder().setBufferDurationsMs(500, 2000, 500, 500).build();
            DataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true);
            DataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory().setCache(GlobalVariable.getVideoCache()).setUpstreamDataSourceFactory(httpDataSourceFactory);
            sharedPlayer = new ExoPlayer.Builder(ctx.getApplicationContext()).setLoadControl(loadControl).setMediaSourceFactory(new DefaultMediaSourceFactory(cacheDataSourceFactory)).build();
            sharedPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        }
        return sharedPlayer;
    }

    public void releasePlayer() {
        if (sharedPlayer != null) {
            sharedPlayer.release();
            sharedPlayer = null;
        }
        currentPlayingHolder = null;
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
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            Video video = videos.get(position);
            holder.currentVideo = video;
            // Cập nhật Metadata (mô tả/hashtag) ngay lập tức
            holder.updateMetadataUI(video);
            Log.d("VideoAdapter", "Payload update (Metadata) for: " + video.getVideoId());
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull VideoViewHolder holder) {
        activeHolders.add(holder);
        if (holder.getBindingAdapterPosition() == currentPosition) holder.playVideo();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull VideoViewHolder holder) {
        activeHolders.remove(holder);
        if (holder == currentPlayingHolder) {
            holder.pauseVideoInternal();
            currentPlayingHolder = null;
        } else {
            holder.pauseVideo();
        }
    }

    @Override
    public void onViewRecycled(@NonNull VideoViewHolder holder) {
        super.onViewRecycled(holder);
        holder.cleanup();
    }

    @Override
    public int getItemCount() { return videos != null ? videos.size() : 0; }

    public void updateCurrentPosition(int pos) { 
        this.currentPosition = pos; 
        this.isManuallyPaused = false; // Reset when switching videos
    }

    public int getCurrentPosition() { return currentPosition; }
    
    public void pauseVideo(int position) {
        if (sharedPlayer != null) {
            sharedPlayer.setPlayWhenReady(false);
        }
        for (VideoViewHolder holder : activeHolders) {
            if (holder.getBindingAdapterPosition() == position) holder.pauseVideo();
        }
    }

    public void resumeVideo() {
        if (sharedPlayer != null && !isManuallyPaused) {
            sharedPlayer.setPlayWhenReady(true);
        }
    }

    public void playVideo(int position) {
        this.isManuallyPaused = false;
        for (VideoViewHolder holder : activeHolders) {
            if (holder.getBindingAdapterPosition() == position) holder.playVideo();
        }
    }

    public void updateWatchCount(int position) {
        if (position < 0 || position >= videos.size()) return;
        Video video = videos.get(position);
        if (video == null) return;
        RecommendationHelper.recordInterest(video.getDescription());

        String videoId = video.getVideoId();
        if (videoId == null || videoId.isEmpty()) return;
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("videos").document(videoId)
                .update("watchCount", FieldValue.increment(1))
                .addOnFailureListener(e -> Log.e(TAG, "Update watchCount failed: " + e.getMessage()));

        firestore.collection("video_summaries").document(videoId)
                .update("watchCount", FieldValue.increment(1))
                .addOnFailureListener(e -> Log.e(TAG, "Update summary watchCount failed: " + e.getMessage()));

        String authorId = video.getAuthorId();
        if (authorId != null && !authorId.isEmpty()) {
            firestore.collection("profiles").document(authorId)
                    .collection("public_videos").document(videoId)
                    .update("watchCount", FieldValue.increment(1))
                    .addOnFailureListener(e -> Log.e(TAG, "Update profile watchCount failed: " + e.getMessage()));
        }

        video.setWatchCount(video.getWatchCount() + 1);
    }

    public class VideoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        StyledPlayerView videoView;
        ImageView imvAvatar, imvLike, imvComment, imvShare, imvMore, imvVolume;
        TextView tvComment, tvFavorites, tvTitle, txvDescription, tvHashtags;
        ProgressBar pbLoading;
        String authorId, boundVideoId;
        boolean isLiked = false;
        Video currentVideo;
        ListenerRegistration likeListener, commentListener;

        private final Player.Listener playerListener = new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if (pbLoading != null) pbLoading.setVisibility(state == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
            }
        };

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
            imvMore = itemView.findViewById(R.id.imvMore);
            imvVolume = itemView.findViewById(R.id.imvVolume);
            pbLoading = itemView.findViewById(R.id.pbLoading);

            imvAvatar.setOnClickListener(this);
            imvLike.setOnClickListener(this);
            imvComment.setOnClickListener(this);
            imvShare.setOnClickListener(this);
            imvMore.setOnClickListener(this);
            if (imvVolume != null) imvVolume.setOnClickListener(this);

            videoView.setOnTouchListener(new OnSwipeTouchListener(itemView.getContext()) {
                @Override public void onSwipeLeft() { moveToProfile(authorId); }

                @Override
                public void onSingleTap() {
                    ExoPlayer player = getSharedPlayer(itemView.getContext());
                    if (videoView.getPlayer() == player && player.isPlaying()) {
                        isManuallyPaused = true;
                        pauseVideo();
                    } else {
                        isManuallyPaused = false;
                        playVideo();
                    }
                }

                @Override
                public void onDoubleTapEvent(MotionEvent e) {
                    if (!isLiked) {
                        toggleLikeLocal();
                    }
                    showHeartAnimation(e);
                }
            });
        }

        public void playVideo() {
            ExoPlayer player = getSharedPlayer(itemView.getContext());
            
            if (currentPlayingHolder != null && currentPlayingHolder != this) {
                currentPlayingHolder.pauseVideoInternal();
            }
            
            currentPlayingHolder = this;
            
            if (videoView.getPlayer() != player) {
                videoView.setPlayer(player);
            }
            
            String videoUri = currentVideo.getVideoUri();
            if (videoUri != null) {
                MediaItem currentItem = player.getCurrentMediaItem();
                String currentUri = (currentItem != null && currentItem.localConfiguration != null) 
                                    ? currentItem.localConfiguration.uri.toString() : "";
                if (!videoUri.equals(currentUri)) {
                    player.setMediaItem(MediaItem.fromUri(videoUri));
                    player.prepare();
                }
            }
            
            player.addListener(playerListener);
            player.setPlayWhenReady(true);
            player.setVolume(isMuted ? 0f : 1f);
            updateVolumeUI();
        }

        public void pauseVideo() {
            ExoPlayer player = getSharedPlayer(itemView.getContext());
            if (videoView.getPlayer() == player) {
                player.setPlayWhenReady(false);
            }
        }

        public void pauseVideoInternal() {
            ExoPlayer player = getSharedPlayer(itemView.getContext());
            player.removeListener(playerListener);
            player.setPlayWhenReady(false); // Fix: Actually pause the player when detached
            if (videoView.getPlayer() == player) {
                videoView.setPlayer(null);
            }
        }

        public void cleanup() {
            ExoPlayer player = getSharedPlayer(itemView.getContext());
            player.removeListener(playerListener);
            if (videoView.getPlayer() == player) {
                videoView.setPlayer(null);
            }
            if (likeListener != null) { likeListener.remove(); likeListener = null; }
            if (commentListener != null) { commentListener.remove(); commentListener = null; }
            boundVideoId = null;
        }

        private void showHeartAnimation(MotionEvent e) {
            if (!(itemView instanceof ViewGroup)) return;
            ViewGroup root = (ViewGroup) itemView;

            final ImageView heart = new ImageView(context);
            heart.setImageResource(R.drawable.ic_favorite);
            heart.setColorFilter(Color.parseColor("#FE2C55"));

            int size = (int) (100 * context.getResources().getDisplayMetrics().density);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size, size);
            
            params.leftMargin = (int) e.getX() - (size / 2);
            params.topMargin = (int) e.getY() - (size / 2);
            heart.setLayoutParams(params);

            root.addView(heart);

            float randomRotation = (float) (Math.random() * 40 - 20);
            heart.setRotation(randomRotation);

            heart.setScaleX(0.2f);
            heart.setScaleY(0.2f);
            heart.setAlpha(1.0f);

            heart.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .rotation(randomRotation * 1.5f)
                    .translationYBy(-100f)
                    .setDuration(400)
                    .withEndAction(() -> {
                        heart.animate()
                                .scaleX(0.8f)
                                .scaleY(0.8f)
                                .alpha(0.0f)
                                .translationYBy(-150f)
                                .setDuration(300)
                                .withEndAction(() -> {
                                    root.removeView(heart);
                                })
                                .start();
                    })
                    .start();
        }

        private void updateLikeUI(boolean liked) {
            imvLike.setImageResource(R.drawable.ic_favorite);
            if (liked) {
                imvLike.setColorFilter(Color.parseColor("#FE2C55")); 
            } else {
                imvLike.setColorFilter(Color.WHITE); 
            }
        }

        private void updateVolumeUI() {
            ExoPlayer player = getSharedPlayer(itemView.getContext());
            if (videoView.getPlayer() == player) {
                player.setVolume(isMuted ? 0f : 1f);
            }
            if (imvVolume != null) {
                imvVolume.setImageResource(isMuted ? R.drawable.ic_baseline_volume_off_24 : R.drawable.ic_baseline_volume_up_24);
            }
        }

        private void toggleVolume() {
            isMuted = !isMuted;
            Toast.makeText(context, isMuted ? "Đã tắt âm" : "Đã bật âm", Toast.LENGTH_SHORT).show();
            for (VideoViewHolder holder : activeHolders) {
                holder.updateVolumeUI();
            }
        }

        public void updateMetadataUI(Video video) {
            if (video == null) return;
            
            String description = video.getDescription();
            String cleanDescription = description != null ? description : "";
            final String REGEX = "#([A-Za-z0-9_\\u00C0-\\u1EF9-]+)";
            
            List<String> hashtagsList = video.getHashtags();
            if (hashtagsList == null || hashtagsList.isEmpty()) {
                hashtagsList = new ArrayList<>();
                if (description != null && description.contains("#")) {
                    Matcher matcher = Pattern.compile(REGEX).matcher(description);
                    while (matcher.find()) {
                        hashtagsList.add(matcher.group(1));
                    }
                }
            }

            if (!hashtagsList.isEmpty()) {
                StringBuilder fullHashtags = new StringBuilder();
                for (String tag : hashtagsList) {
                    fullHashtags.append("#").append(tag).append(" ");
                }
                
                SpannableString spannableString = new SpannableString(fullHashtags.toString().trim());
                int start = 0;
                for (String tag : hashtagsList) {
                    String tagWithHash = "#" + tag;
                    int tagStart = spannableString.toString().indexOf(tagWithHash, start);
                    if (tagStart != -1) {
                        int tagEnd = tagStart + tagWithHash.length();
                        spannableString.setSpan(new ClickableSpan() {
                            @Override
                            public void onClick(@NonNull View widget) {
                                if (context instanceof HomeScreenActivity) {
                                    ((HomeScreenActivity) context).handleSearchClick(tagWithHash);
                                }
                            }
                            @Override
                            public void updateDrawState(@NonNull TextPaint ds) {
                                super.updateDrawState(ds);
                                ds.setUnderlineText(false);
                                ds.setColor(Color.WHITE);
                                ds.setFakeBoldText(true);
                            }
                        }, tagStart, tagEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        start = tagEnd;
                    }
                }
                
                tvHashtags.setText(spannableString);
                tvHashtags.setMovementMethod(LinkMovementMethod.getInstance());
                tvHashtags.setVisibility(View.VISIBLE);
                cleanDescription = cleanDescription.replaceAll(REGEX, "").trim();
            } else {
                tvHashtags.setVisibility(View.GONE);
            }
            txvDescription.setText(cleanDescription.isEmpty() ? (description != null ? description : "") : cleanDescription);
        }

        public void setVideoObjects(Video video, int position) {
            if (video == null) return;
            this.currentVideo = video;
            this.authorId = video.getAuthorId();

            updateMetadataUI(video);

            if (boundVideoId == null || !boundVideoId.equals(video.getVideoId())) {
                this.boundVideoId = video.getVideoId();
                
                imvAvatar.setImageResource(R.drawable.default_avatar);
                String username = video.getUsername();
                tvTitle.setText(username != null && !username.isEmpty() ? "@" + username : "@User");
                FirebaseFirestore.getInstance().collection("profiles").document(authorId)
                        .get().addOnSuccessListener(doc -> {
                            if (doc.exists() && video.getVideoId().equals(boundVideoId)) {
                                String realUsername = doc.getString("username");
                                String avatarUrl = doc.getString("avatarUrl");
                                if (realUsername != null && !realUsername.isEmpty()) {
                                    tvTitle.setText("@" + realUsername);
                                }
                                if (avatarUrl != null && !avatarUrl.isEmpty() && context != null) {
                                    Glide.with(context).load(avatarUrl).placeholder(R.drawable.default_avatar).circleCrop().into(imvAvatar);
                                }
                            }
                        });

                setupRealtimeListeners(video.getVideoId());
            }
            if (position == currentPosition) {
                playVideo();
            } else {
                pauseVideo();
            }
            updateVolumeUI();
        }

        private void setupRealtimeListeners(String vid) {
            if (likeListener != null) likeListener.remove();
            if (commentListener != null) commentListener.remove();
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            likeListener = firestore.collection("likes").document(vid).addSnapshotListener((snapshot, error) -> {
                if (snapshot != null && snapshot.exists() && vid.equals(boundVideoId)) {
                    Map<String, Object> data = snapshot.getData();
                    tvFavorites.setText(String.valueOf(likesCount(data)));
                    String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
                    isLiked = !currentUid.isEmpty() && data != null && data.containsKey(currentUid);
                    updateLikeUI(isLiked);
                }
            });

            commentListener = firestore.collection("comments").whereEqualTo("videoId", vid).addSnapshotListener((snapshots, error) -> {
                if (snapshots != null && vid.equals(boundVideoId)) {
                    tvComment.setText(String.valueOf(snapshots.size()));
                }
            });
        }
        
        private int likesCount(Map<String, Object> data) {
            if (data == null) return 0;
            int count = 0;
            for (Object val : data.values()) { if (val instanceof Boolean && (Boolean)val) count++; }
            return count;
        }

        @Override public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.videoView) {
                ExoPlayer player = getSharedPlayer(itemView.getContext());
                if (videoView.getPlayer() == player && player.isPlaying()) pauseVideo();
                else playVideo();
            } else if (id == R.id.imvLike) {
                toggleLikeLocal();
            } else if (id == R.id.imvComment) {
                showComments();
            } else if (id == R.id.imvAvatar) {
                moveToProfile(authorId);
            } else if (id == R.id.imvShare) {
                shareDemo();
            } else if (id == R.id.imvMore) {
                showMoreOptions();
            } else if (id == R.id.imvVolume) {
                toggleVolume();
            }
        }

        private void toggleLikeLocal() {
            String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
            if (currentUid.isEmpty()) {
                Toast.makeText(context, "Vui lòng đăng nhập để thích video!", Toast.LENGTH_SHORT).show();
                Intent loginIntent = new Intent(context, MainActivity.class);
                context.startActivity(loginIntent);
                return;
            }
            isLiked = !isLiked;
            updateLikeUI(isLiked);
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            if (isLiked) {
                Map<String, Object> likeData = new HashMap<>();
                likeData.put(currentUid, true);
                firestore.collection("likes").document(boundVideoId).set(likeData, com.google.firebase.firestore.SetOptions.merge());
                firestore.collection("videos").document(boundVideoId).update("totalLikes", com.google.firebase.firestore.FieldValue.increment(1));
                firestore.collection("profiles").document(authorId).update("likes", com.google.firebase.firestore.FieldValue.increment(1));
                
                // Gửi thông báo Like
                firestore.collection("profiles").document(currentUid).get().addOnSuccessListener(doc -> {
                    if (doc.exists() && boundVideoId.equals(currentVideo.getVideoId())) {
                        String name = doc.getString("username");
                        com.example.tiktokcloneproject.model.Notification.pushNotification(
                            name != null ? name : "Ai đó", 
                            authorId, 
                            com.example.tiktokcloneproject.helper.StaticVariable.LIKE
                        );
                    }
                });
            } else {
                Map<String, Object> updates = new HashMap<>();
                updates.put(currentUid, com.google.firebase.firestore.FieldValue.delete());
                firestore.collection("likes").document(boundVideoId).update(updates);
                firestore.collection("videos").document(boundVideoId).update("totalLikes", com.google.firebase.firestore.FieldValue.increment(-1));
                firestore.collection("profiles").document(authorId).update("likes", com.google.firebase.firestore.FieldValue.increment(-1));
            }
        }

        private void showMoreOptions() {
            String currentUid = FirebaseAuth.getInstance().getUid();
            boolean isOwner = currentUid != null && currentUid.equals(authorId);

            List<String> optionsList = new ArrayList<>(Arrays.asList("Báo cáo video này", "Lưu video", "Không quan tâm"));
            if (isOwner) {
                optionsList.add("Sửa mô tả");
                optionsList.add("Xóa video này");
            }

            String[] options = optionsList.toArray(new String[0]);
            new AlertDialog.Builder(context).setItems(options, (dialog, which) -> {
                String selected = options[which];
                if (selected.equals("Báo cáo video này")) {
                    showReportDialog();
                } else if (selected.equals("Xóa video này")) {
                    showDeleteConfirmation();
                } else if (selected.equals("Sửa mô tả")) {
                    Intent editIntent = new Intent(context, DescriptionVideoActivity.class);
                    editIntent.putExtra("isEditMode", true);
                    editIntent.putExtra("videoId", boundVideoId);
                    context.startActivity(editIntent);
                } else {
                    Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
                }
            }).show();
        }

        private void showDeleteConfirmation() {
            new AlertDialog.Builder(context)
                    .setTitle("Xóa video")
                    .setMessage("Bạn có chắc chắn muốn xóa video này không?")
                    .setPositiveButton("Xóa", (dialog, which) -> deleteVideo())
                    .setNegativeButton("Hủy", null)
                    .show();
        }

        private void deleteVideo() {
            FirebaseFirestore.getInstance().collection("videos").document(boundVideoId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Đã xóa video", Toast.LENGTH_SHORT).show();
                        int pos = getBindingAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            videos.remove(pos);
                            notifyItemRemoved(pos);
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Xóa thất bại", Toast.LENGTH_SHORT).show());
        }

        private void showReportDialog() {
            String[] reasons = {"Nội dung nhạy cảm", "Spam", "Quấy rối"};
            new AlertDialog.Builder(context).setTitle("Lý do báo cáo").setItems(reasons, (dialog, which) -> submitReport(reasons[which])).show();
        }

        private void submitReport(String reason) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;
            String reportId = UUID.randomUUID().toString();
            Report report = new Report(reportId, user.getUid(), boundVideoId, "video", reason, currentVideo.getDescription());
            FirebaseFirestore.getInstance().collection("reports").document(reportId).set(report.toMap())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Báo cáo thành công!", Toast.LENGTH_SHORT).show();
                    if (authorId != null && !authorId.isEmpty()) {
                        com.example.tiktokcloneproject.model.Notification.pushNotification(
                            "Hệ thống", 
                            authorId, 
                            "APPEAL_REQUEST|" + reportId + "|" + reason
                        );
                    }
                });
        }

        private void showComments() {
            String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
            if (currentUid.isEmpty()) {
                Toast.makeText(context, "Vui lòng đăng nhập để bình luận!", Toast.LENGTH_SHORT).show();
                Intent loginIntent = new Intent(context, MainActivity.class);
                context.startActivity(loginIntent);
                return;
            }
            Intent intent = new Intent(context, CommentActivity.class);
            intent.putExtra("videoId", boundVideoId);
            intent.putExtra("authorId", authorId); // Đã thêm: truyền ID tác giả video
            context.startActivity(intent);
        }

        private void moveToProfile(String uid) { 
            Intent intent = new Intent(context, ProfileActivity.class); 
            intent.putExtra("id", uid);
            context.startActivity(intent); 
        }

        private void shareToApp(String packageName, String videoUri) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, videoUri);
            if (packageName != null) {
                intent.setPackage(packageName);
            }
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                if (packageName != null) {
                    String appName = "ứng dụng";
                    if (packageName.contains("zalo")) appName = "Zalo";
                    else if (packageName.contains("orca")) appName = "Messenger";
                    else if (packageName.contains("katana")) appName = "Facebook";
                    Toast.makeText(context, appName + " chưa được cài đặt trên thiết bị!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Không tìm thấy ứng dụng chia sẻ!", Toast.LENGTH_SHORT).show();
                }
            }
        }

        private void shareDemo() {
            final Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.share_video_layout);

            String shareUrl = "https://hubertphung.github.io/vifarm-share-page/?id=" + currentVideo.getVideoId();
            
            dialog.findViewById(R.id.btnCopyURL).setOnClickListener(view -> {
                ClipboardManager cb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                cb.setPrimaryClip(ClipData.newPlainText("video-link", shareUrl));
                Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });

            dialog.findViewById(R.id.btnShareZalo).setOnClickListener(view -> {
                shareToApp("com.zing.zalo", shareUrl);
                dialog.dismiss();
            });

            dialog.findViewById(R.id.btnShareMessenger).setOnClickListener(view -> {
                shareToApp("com.facebook.orca", shareUrl);
                dialog.dismiss();
            });

            dialog.findViewById(R.id.btnShareFacebook).setOnClickListener(view -> {
                shareToApp("com.facebook.katana", shareUrl);
                dialog.dismiss();
            });

            dialog.findViewById(R.id.btnShareSystem).setOnClickListener(view -> {
                shareToApp(null, shareUrl);
                dialog.dismiss();
            });

            // Action Row Logic
            String currentUid = FirebaseAuth.getInstance().getUid();
            boolean isOwner = currentUid != null && currentUid.equals(authorId);

            if (isOwner) {
                dialog.findViewById(R.id.llActionEdit).setVisibility(View.VISIBLE);
                dialog.findViewById(R.id.llActionDelete).setVisibility(View.VISIBLE);
            }

            dialog.findViewById(R.id.btnActionReport).setOnClickListener(view -> {
                showReportDialog();
                dialog.dismiss();
            });

            dialog.findViewById(R.id.btnActionSave).setOnClickListener(view -> {
                Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });

            dialog.findViewById(R.id.btnActionEdit).setOnClickListener(view -> {
                Intent editIntent = new Intent(context, DescriptionVideoActivity.class);
                editIntent.putExtra("isEditMode", true);
                editIntent.putExtra("videoId", boundVideoId);
                context.startActivity(editIntent);
                dialog.dismiss();
            });

            dialog.findViewById(R.id.btnActionDelete).setOnClickListener(view -> {
                showDeleteConfirmation();
                dialog.dismiss();
            });

            // Load direct share friends list
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String myUid = currentUser.getUid();
                DatabaseReference chatListRef = FirebaseHelper.getDatabase().getReference("ChatList").child(myUid);
                chatListRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;
                        List<ChatPartner> partners = new ArrayList<>();
                        int totalPartners = (int) snapshot.getChildrenCount();
                        if (totalPartners == 0) return;
                        final int[] loadedCount = {0};

                        for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                            String partnerId = chatSnapshot.getKey();
                            if (partnerId == null) {
                                loadedCount[0]++;
                                continue;
                            }
                            String roomId = (myUid.compareTo(partnerId) < 0) ? myUid + "_" + partnerId : partnerId + "_" + myUid;
                            DatabaseReference messagesRef = FirebaseHelper.getDatabase().getReference("Chats").child(roomId);
                            messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot msgSnapshot) {
                                    long msgCount = msgSnapshot.getChildrenCount();
                                    long lastTimestamp = 0;
                                    if (chatSnapshot.hasChild("timestamp")) {
                                        Object tsObj = chatSnapshot.child("timestamp").getValue();
                                        if (tsObj instanceof Long) {
                                            lastTimestamp = (Long) tsObj;
                                        }
                                    }
                                    partners.add(new ChatPartner(partnerId, msgCount, lastTimestamp));
                                    loadedCount[0]++;
                                    if (loadedCount[0] >= totalPartners) {
                                        sortAndDisplayPartners(dialog, partners, shareUrl);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    loadedCount[0]++;
                                    if (loadedCount[0] >= totalPartners) {
                                        sortAndDisplayPartners(dialog, partners, shareUrl);
                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            dialog.findViewById(R.id.txvCancelInSharedPlace).setOnClickListener(view -> dialog.cancel());
            dialog.show();
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setGravity(Gravity.BOTTOM);
                window.setWindowAnimations(R.style.DialogAnimation);
                window.getDecorView().setPadding(0, 0, 0, 0);
            }
        }
    }

    private static class ChatPartner {
        String id;
        long messageCount;
        long lastTimestamp;

        ChatPartner(String id, long messageCount, long lastTimestamp) {
            this.id = id;
            this.messageCount = messageCount;
            this.lastTimestamp = lastTimestamp;
        }
    }

    private void sortAndDisplayPartners(Dialog dialog, List<ChatPartner> partners, String shareUrl) {
        Collections.sort(partners, new Comparator<ChatPartner>() {
            @Override
            public int compare(ChatPartner p1, ChatPartner p2) {
                if (p2.messageCount != p1.messageCount) {
                    return Long.compare(p2.messageCount, p1.messageCount);
                }
                return Long.compare(p2.lastTimestamp, p1.lastTimestamp);
            }
        });

        ViewGroup friendsContainer = dialog.findViewById(R.id.llFriendsContainer);
        if (friendsContainer == null || partners.isEmpty()) return;

        Context ctx = dialog.getContext();
        LayoutInflater inflater = LayoutInflater.from(ctx);

        dialog.findViewById(R.id.tvSendToHeader).setVisibility(View.VISIBLE);
        dialog.findViewById(R.id.hsvFriends).setVisibility(View.VISIBLE);
        dialog.findViewById(R.id.vDividerFriends).setVisibility(View.VISIBLE);

        for (ChatPartner partner : partners) {
            View itemView = inflater.inflate(R.layout.item_share_friend, friendsContainer, false);
            ImageView ivAvatar = itemView.findViewById(R.id.ivFriendAvatar);
            TextView tvName = itemView.findViewById(R.id.tvFriendName);

            FirebaseFirestore.getInstance().collection("profiles").document(partner.id)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            String avatarUrl = documentSnapshot.getString("avatarUrl");
                            
                            tvName.setText(username != null ? username : "User");
                            Glide.with(ctx)
                                    .load(avatarUrl != null && !avatarUrl.isEmpty() ? avatarUrl : R.drawable.default_avatar)
                                    .placeholder(R.drawable.default_avatar)
                                    .into(ivAvatar);
                        }
                    });

            itemView.setOnClickListener(v -> {
                sendDirectVideoShare(partner.id, shareUrl);
                dialog.dismiss();
            });

            friendsContainer.addView(itemView);
        }
    }

    private void sendDirectVideoShare(String receiverId, String shareUrl) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        String senderId = currentUser.getUid();
        String roomId = (senderId.compareTo(receiverId) < 0) ? senderId + "_" + receiverId : receiverId + "_" + senderId;
        
        DatabaseReference ref = FirebaseHelper.getDatabase().getReference();
        long timestamp = System.currentTimeMillis();
        
        String msgKey = ref.child("Chats").child(roomId).push().getKey();
        if (msgKey == null) return;
        
        String messageText = "Đã chia sẻ một video: " + shareUrl;
        ChatMessage chatMessage = new ChatMessage(senderId, receiverId, messageText, timestamp, "text");
        
        Map<String, Object> messageValues = new HashMap<>();
        messageValues.put("/Chats/" + roomId + "/" + msgKey, chatMessage);

        Map<String, Object> chatListData = new HashMap<>();
        chatListData.put("id", receiverId);
        chatListData.put("lastMessage", messageText);
        chatListData.put("timestamp", timestamp);
        messageValues.put("/ChatList/" + senderId + "/" + receiverId, chatListData);

        Map<String, Object> chatListDataReceiver = new HashMap<>();
        chatListDataReceiver.put("id", senderId);
        chatListDataReceiver.put("lastMessage", messageText);
        chatListDataReceiver.put("timestamp", timestamp);
        messageValues.put("/ChatList/" + receiverId + "/" + senderId, chatListDataReceiver);

        ref.updateChildren(messageValues).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(context, "Đã gửi video thành công!", Toast.LENGTH_SHORT).show();
                FirebaseFirestore.getInstance().collection("profiles").document(senderId)
                        .get().addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String name = doc.getString("username");
                                com.example.tiktokcloneproject.model.Notification.pushNotification(
                                    name != null ? name : "Ai đó", 
                                    receiverId, 
                                    com.example.tiktokcloneproject.helper.StaticVariable.CHAT
                                );
                            }
                        });
            } else {
                Toast.makeText(context, "Gửi video thất bại!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
