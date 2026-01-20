package com.back.domain.chat.dto;

import java.time.LocalDateTime;

public record ChatDto(
        int id,
        int itemId, // 상품 ID
        String roomId,   // 방 번호
        String sender,   // 보낸 사람
        String message,  // 메시지 내용
        LocalDateTime createDate, // 보낸 시간
        boolean isRead
) {
}