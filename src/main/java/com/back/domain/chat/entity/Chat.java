package com.back.domain.chat.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chat extends BaseEntity {
    private Long itemId;
    private String roomId;
    private String sender;
    @Column(columnDefinition = "TEXT")
    private String message;
    private boolean isRead; // 읽음 여부

    @Builder
    public Chat(Long itemId, String roomId, String sender, String message, boolean isRead) {
        this.itemId = itemId;
        this.roomId = roomId;
        this.sender = sender;
        this.message = message;
        this.isRead = isRead;
    }
}