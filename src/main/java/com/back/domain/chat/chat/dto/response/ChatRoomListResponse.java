package com.back.domain.chat.chat.dto.response;

import com.back.domain.chat.chat.entity.ChatRoomType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoomListResponse {
    private String roomId;

    // 상대방 정보
    private Integer opponentId;
    private String opponentNickname;
    private String opponentProfileImageUrl; // 유저 프로필 사진
    private Double opponentMannerTemp;

    // 마지막 메세지 정보
    private String lastMessage;
    private LocalDateTime lastMessageDate;
    private Integer unreadCount; // 안 읽은 메세지 수

    // 상품 정보
    private Integer itemId;
    private String itemName;
    private String itemImageUrl; // 상품 썸네일
    private Integer itemPrice; // 물품 가격
    private ChatRoomType txType; // Auction or Post
}
