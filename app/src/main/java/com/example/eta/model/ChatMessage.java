package com.example.eta.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatMessage {
    public static final int TYPE_MESSAGE = 0; // 일반 메시지
    public static final int TYPE_SYSTEM = 1; // 시스템 메시지

    private String senderId;
    private String senderNickname;
    private String message;
    private long timestamp;
    private int messageType;

    public ChatMessage() {
        this.messageType = TYPE_MESSAGE;
    }

    public ChatMessage(String senderId, String senderNickname, String message, long timestamp) {
        this.senderId = senderId;
        this.senderNickname = senderNickname;
        this.message = message;
        this.timestamp = timestamp;
        this.messageType = TYPE_MESSAGE;
    }

    // 시스템 메시지용 생성자
    public ChatMessage(String senderId, String senderNickname, String message, long timestamp, int messageType) {
        this.senderId = senderId;
        this.senderNickname = senderNickname;
        this.message = message;
        this.timestamp = timestamp;
        this.messageType = messageType;
    }

    // Getter와 Setter 메소드
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderNickname() { return senderNickname; }
    public void setSenderNickname(String senderNickname) { this.senderNickname = senderNickname; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getMessageType() { return messageType; }
    public void setMessageType(int messageType) { this.messageType = messageType; }

    public boolean isSystemMessage() {
        return messageType == TYPE_SYSTEM;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
