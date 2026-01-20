package com.back.domain.chat.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Table(name = "chat")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chat extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", referencedColumnName = "roomId", nullable = false)
    private ChatRoom chatRoom;

    @Column(nullable = false)
    private String sender;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false; // 읽음 여부

    @Builder
    public Chat(ChatRoom chatRoom, String sender, String message, boolean isRead) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.message = message;
        this.isRead = isRead;
    }
}