package com.back.domain.chat.dto;

import java.time.LocalDateTime;

public record ChatDto(
        String roomId,   // 방 번호
        String sender,   // 보낸 사람
        String message,  // 메시지 내용
        LocalDateTime sendTime // 보낸 시간
) {
}