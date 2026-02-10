package com.back.domain.chat.chat.dto.response;

import com.back.domain.chat.chat.entity.ChatRoomType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatNotification {
    private String type;           // "NEW_ROOM" or "NEW_MESSAGE"
    private String roomId;

    // 상대방 정보 (알림을 받는 사람 기준)
    private Integer opponentId;
    private String opponentNickname;
    private String opponentProfileImageUrl;

    // 메시지 정보
    private String lastMessage;
    private LocalDateTime lastMessageDate;
    private Integer unreadCount;

    // 상품 정보
    private Integer itemId;
    private String itemName;
    private String itemImageUrl;
    private Integer itemPrice;
    private ChatRoomType txType;
}