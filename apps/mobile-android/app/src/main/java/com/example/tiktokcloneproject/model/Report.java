package com.example.tiktokcloneproject.model;

import java.util.HashMap;
import java.util.Map;

public class Report {
    private String reportId;
    private String reporterId;
    private String targetId; // videoId or userId
    private String targetType; // "video" or "user"
    private String reason;
    private String videoTitle;
    private String status; // "pending", "resolved", "dismissed"
    private long timestamp;
    private String appeal; // Content of user's appeal

    public Report() {}

    public Report(String reportId, String reporterId, String targetId, String targetType, String reason, String videoTitle) {
        this.reportId = reportId;
        this.reporterId = reporterId;
        this.targetId = targetId;
        this.targetType = targetType;
        this.reason = reason;
        this.videoTitle = videoTitle;
        this.status = "pending";
        this.timestamp = System.currentTimeMillis();
    }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getVideoTitle() { return videoTitle; }
    public void setVideoTitle(String videoTitle) { this.videoTitle = videoTitle; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getAppeal() { return appeal; }
    public void setAppeal(String appeal) { this.appeal = appeal; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("reportId", reportId);
        map.put("reporterId", reporterId);
        map.put("targetId", targetId);
        map.put("targetType", targetType);
        map.put("reason", reason);
        map.put("videoTitle", videoTitle);
        map.put("status", status);
        map.put("timestamp", timestamp);
        map.put("appeal", appeal);
        return map;
    }
}
