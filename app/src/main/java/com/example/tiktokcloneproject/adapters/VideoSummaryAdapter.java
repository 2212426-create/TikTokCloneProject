package com.example.tiktokcloneproject.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.activity.VideoActivity;
import com.example.tiktokcloneproject.model.VideoSummary;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class VideoSummaryAdapter extends RecyclerView.Adapter<VideoSummaryAdapter.ViewHolder> {

    private ArrayList<VideoSummary> mData;
    private LayoutInflater mInflater;
    private Context mainContext;

    public VideoSummaryAdapter(Context context, ArrayList<VideoSummary> data) {
        this.mainContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.video_summary_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (mData == null || position >= mData.size()) return;
        VideoSummary item = mData.get(position);
        if (item == null) return;

        if (holder.viewCount != null) {
            holder.viewCount.setText(String.valueOf(item.getWatchCount() != null ? item.getWatchCount() : 0));
        }
        
        if (holder.thumbnail != null && item.getThumbnailUri() != null && !item.getThumbnailUri().isEmpty()) {
            try {
                String thumbUri = item.getThumbnailUri();
                Object loadTarget;
                boolean isVideo = thumbUri.toLowerCase().endsWith(".mp4");
                
                // Cloudinary: Get the very first frame (so_0) as requested
                if (thumbUri.contains("cloudinary.com") && isVideo) {
                    thumbUri = thumbUri.replace(".mp4", ".jpg");
                    if (thumbUri.contains("/upload/")) {
                        thumbUri = thumbUri.replace("/upload/", "/upload/so_0/");
                    }
                    loadTarget = thumbUri;
                } else if (thumbUri.startsWith("gs://")) {
                    loadTarget = FirebaseStorage.getInstance().getReferenceFromUrl(thumbUri);
                } else if (thumbUri.startsWith("https://") && thumbUri.contains("firebasestorage.googleapis.com")) {
                    loadTarget = FirebaseStorage.getInstance().getReferenceFromUrl(thumbUri);
                } else {
                    loadTarget = thumbUri;
                }

                if (mainContext != null) {
                    RequestOptions options = new RequestOptions()
                            .placeholder(R.drawable.splash_background)
                            .error(R.drawable.splash_background)
                            .centerCrop();
                    
                    // If not Cloudinary, tell Glide to extract first frame (0)
                    if (isVideo && !thumbUri.contains("cloudinary.com")) {
                        options = options.frame(0);
                    }

                    Glide.with(mainContext)
                            .load(loadTarget)
                            .apply(options)
                            .into(holder.thumbnail);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (item.getVideoId() == null) return;
            Intent intent = new Intent(v.getContext(), VideoActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("videoId", item.getVideoId());
            intent.putExtras(bundle);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return mData != null ? mData.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView viewCount;
        ImageView thumbnail;

        ViewHolder(View itemView) {
            super(itemView);
            viewCount = itemView.findViewById(R.id.view_count);
            thumbnail = itemView.findViewById(R.id.image_thumbnail);
        }
    }
}
