package com.example.eta.model;

public class UserData {
    private String nickname;
    private long createdAt;
    private long lastActiveAt;

    public UserData() {
        // Firebase용 기본 생성자
    }

    public UserData(String nickname, long createdAt) {
        this.nickname = nickname;
        this.createdAt = createdAt;
        this.lastActiveAt = createdAt;
    }

    // Getter와 Setter
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(long lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }
}
