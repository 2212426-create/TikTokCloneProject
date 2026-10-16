package com.example.tiktokcloneproject.model;

public class ChatMessage {
    private String senderId;
    private String receiverId;
    private String message;
    private long timestamp;
    private String type; // "text", "image", "video"

    public ChatMessage() {
    }

    public ChatMessage(String senderId, String receiverId, String message, long timestamp, String type) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.type = type;
    }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
