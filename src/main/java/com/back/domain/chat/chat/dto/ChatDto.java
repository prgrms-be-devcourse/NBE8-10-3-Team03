package com.back.domain.chat.chat.dto;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public record ChatDto(
        int id,
        int itemId, // 상품 ID
        String roomId,   // 방 번호
        String sender,   // 보낸 사람
        String message,  // 메시지 내용
        LocalDateTime createDate, // 보낸 시간
        List<MultipartFile> images,
        boolean isRead
) {
}