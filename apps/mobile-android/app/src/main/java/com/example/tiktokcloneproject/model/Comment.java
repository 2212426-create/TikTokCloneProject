package com.example.tiktokcloneproject.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Comment {
    private String commentId, videoId, authorId, content;
    private int totalLikes, totalReplies;
    private ArrayList<String> replyIds;
    private String parentId;
    private String parentUsername;

    public Comment() {
        replyIds = new ArrayList<>();
    }

    public Comment(String commentId, String videoId, String authorId, String content) {
        this.commentId = commentId;
        this.videoId = videoId;
        this.authorId = authorId;
        this.content = content;
        this.totalLikes = 0;
        this.totalReplies = 0;
        this.replyIds = new ArrayList<>();
        this.parentId = "";
        this.parentUsername = "";
    }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getTotalLikes() { return totalLikes; }
    public void setTotalLikes(int totalLikes) { this.totalLikes = totalLikes; }

    public int getTotalReplies() { return totalReplies; }
    public void setTotalReplies(int totalReplies) { this.totalReplies = totalReplies; }

    public ArrayList<String> getReplyIds() { return replyIds; }
    public void setReplyIds(ArrayList<String> replyIds) { this.replyIds = replyIds; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getParentUsername() { return parentUsername; }
    public void setParentUsername(String parentUsername) { this.parentUsername = parentUsername; }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("commentId", commentId);
        result.put("videoId", videoId);
        result.put("authorId", authorId);
        result.put("totalLikes", totalLikes);
        result.put("totalReplies", totalReplies);
        result.put("replyIds", replyIds);
        result.put("content", content);
        result.put("parentId", parentId);
        result.put("parentUsername", parentUsername);
        return result;
    }
}
