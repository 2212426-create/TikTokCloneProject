package com.example.tiktokcloneproject.model;

import java.util.HashMap;
import java.util.Map;

public class Video {
    private String videoId, videoUri, authorId, description, username;
    private int totalLikes, totalComments, watchCount;
    private long timestamp;

    public Video() {
    }

    public Video(String videoId, String videoUri, String authorId, String username, String description, long timestamp) {
        this.videoId = videoId;
        this.videoUri = videoUri;
        this.authorId = authorId;
        this.username = username;
        this.description = description;
        this.timestamp = timestamp;
        this.totalLikes = 0;
        this.totalComments = 0;
        this.watchCount = 0;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }

    public String getVideoUri() { return videoUri; }
    public void setVideoUri(String videoUri) { this.videoUri = videoUri; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getTotalLikes() { return totalLikes; }
    public void setTotalLikes(int totalLikes) { this.totalLikes = totalLikes; }

    public int getTotalComments() { return totalComments; }
    public void setTotalComments(int totalComments) { this.totalComments = totalComments; }

    public int getWatchCount() { return watchCount; }
    public void setWatchCount(int watchCount) { this.watchCount = watchCount; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("videoId", videoId);
        result.put("videoUri", videoUri);
        result.put("authorId", authorId);
        result.put("username", username);
        result.put("description", description);
        result.put("totalComments", totalComments);
        result.put("totalLikes", totalLikes);
        result.put("watchCount", watchCount);
        result.put("timestamp", timestamp);
        return result;
    }
}
