package com.example.eta.model;

public class AppointmentRoom {
    private String roomId;
    private String appointmentName;
    private String creatorNickname;
    private long createdAt;
    private int participantCount;
    private String chatRoomCode; // 채팅방 코드 필드 추가

    public AppointmentRoom() {
        // Firebase용 기본 생성자
    }

    public AppointmentRoom(String roomId, String appointmentName, String creatorNickname,
                           long createdAt, int participantCount, String chatRoomCode) {
        this.roomId = roomId;
        this.appointmentName = appointmentName;
        this.creatorNickname = creatorNickname;
        this.createdAt = createdAt;
        this.participantCount = participantCount;
        this.chatRoomCode = chatRoomCode;
    }

    // Getter와 Setter
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getAppointmentName() {
        return appointmentName;
    }

    public void setAppointmentName(String appointmentName) {
        this.appointmentName = appointmentName;
    }

    public String getCreatorNickname() {
        return creatorNickname;
    }

    public void setCreatorNickname(String creatorNickname) {
        this.creatorNickname = creatorNickname;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public int getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(int participantCount) {
        this.participantCount = participantCount;
    }

    public String getChatRoomCode() {
        return chatRoomCode;
    }

    public void setChatRoomCode(String chatRoomCode) {
        this.chatRoomCode = chatRoomCode;
    }
}
