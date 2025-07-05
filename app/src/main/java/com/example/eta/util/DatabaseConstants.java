package com.example.eta.util;

public class DatabaseConstants {

    // 데이터베이스 노드 이름
    public static final String NODE_USERS = "users";
    public static final String NODE_USERNAMES = "usernames";  // 닉네임 고유성 관리용
    public static final String NODE_APPOINTMENTS = "appointments";
    public static final String NODE_APPOINTMENT_ROOMS = "appointmentRooms";
    public static final String NODE_CHAT_ROOMS = "chatRooms";
    public static final String NODE_MESSAGES = "messages";

    // NODE_NICKNAMES는 NODE_USERNAMES와 중복이므로 제거하거나 통일
    // public static final String NODE_NICKNAMES = "nicknames"; // 제거됨

    // 필드 이름
    public static final String FIELD_NICKNAME = "nickname";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_LAST_ACTIVE_AT = "lastActiveAt";
    public static final String FIELD_TIMESTAMP = "timestamp";
    public static final String FIELD_MESSAGE = "message";
    public static final String FIELD_SENDER_ID = "senderId";
    public static final String FIELD_ROOM_ID = "roomId";
    public static final String FIELD_APPOINTMENT_NAME = "appointmentName";
    public static final String FIELD_CREATOR_NICKNAME = "creatorNickname";
    public static final String FIELD_PARTICIPANT_COUNT = "participantCount";

    // 추가 필드들
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_PARTICIPANTS = "participants";
    public static final String FIELD_SENDER_NICKNAME = "senderNickname";
    public static final String FIELD_IS_SYSTEM_MESSAGE = "isSystemMessage";

    // 채팅 관련 노드
    public static final String NODE_CHATS = "chats";

    // 기본 생성자 방지
    private DatabaseConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
