package com.example.tiktokcloneproject.model;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class User implements Serializable {
    private String userId;
    private String username;
    private String avatarUrl;
    private String email;
    private String phone;
    private String birthdate;
    private boolean isPrivate;
    private long followers;
    private long following;
    private long likes;

    public User() {}

    public User(String userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public User(String userId, String username, String avatarUrl, String email) {
        this.userId = userId;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.email = email;
    }

    @PropertyName("userId")
    public String getUserId() { return userId; }
    @PropertyName("userId")
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("username")
    public String getUsername() { return username; }
    @PropertyName("username")
    public void setUsername(String username) { this.username = username; }

    @PropertyName("avatarUrl")
    public String getAvatarUrl() { return avatarUrl; }
    @PropertyName("avatarUrl")
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    @PropertyName("email")
    public String getEmail() { return email; }
    @PropertyName("email")
    public void setEmail(String email) { this.email = email; }

    @PropertyName("phone")
    public String getPhone() { return phone; }
    @PropertyName("phone")
    public void setPhone(String phone) { this.phone = phone; }

    @PropertyName("birthdate")
    public String getBirthdate() { return birthdate; }
    @PropertyName("birthdate")
    public void setBirthdate(String birthdate) { this.birthdate = birthdate; }

    @PropertyName("isPrivate")
    public boolean isPrivate() { return isPrivate; }
    @PropertyName("isPrivate")
    public void setPrivate(boolean aPrivate) { isPrivate = aPrivate; }

    @PropertyName("followers")
    public long getFollowers() { return followers; }
    @PropertyName("followers")
    public void setFollowers(long followers) { this.followers = followers; }

    @PropertyName("following")
    public long getFollowing() { return following; }
    @PropertyName("following")
    public void setFollowing(long following) { this.following = following; }

    @PropertyName("likes")
    public long getLikes() { return likes; }
    @PropertyName("likes")
    public void setLikes(long likes) { this.likes = likes; }

    // Fallback cho trường avatarName hoặc avatarUri cũ
    @PropertyName("avatarName")
    public String getAvatarName() { return avatarUrl; }
    @PropertyName("avatarName")
    public void setAvatarName(String avatarName) { if (this.avatarUrl == null) this.avatarUrl = avatarName; }

    @PropertyName("avatarUri")
    public String getAvatarUri() { return avatarUrl; }
    @PropertyName("avatarUri")
    public void setAvatarUri(String avatarUri) { if (this.avatarUrl == null) this.avatarUrl = avatarUri; }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("username", username);
        result.put("avatarUrl", avatarUrl);
        result.put("email", email);
        result.put("phone", phone);
        result.put("isPrivate", isPrivate);
        result.put("followers", followers);
        result.put("following", following);
        result.put("likes", likes);
        return result;
    }
}
