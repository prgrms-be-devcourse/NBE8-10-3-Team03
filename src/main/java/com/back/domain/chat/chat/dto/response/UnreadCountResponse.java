package com.back.domain.chat.chat.dto.response;

public record UnreadCountResponse(
    String roomId,
    Long count
) {
}