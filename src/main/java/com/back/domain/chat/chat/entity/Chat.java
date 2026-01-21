package com.back.domain.chat.chat.entity;

import com.back.domain.post.post.entity.PostImage;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Table(name = "chat")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chat extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", referencedColumnName = "roomId", nullable = false)
    private ChatRoom chatRoom;

    @Column(name = "sender_id", nullable = false)
    private Integer senderId;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false; // 읽음 여부

    @Builder
    public Chat(ChatRoom chatRoom, Integer senderId, String message, boolean isRead) {
        this.chatRoom = chatRoom;
        this.senderId = senderId;
        this.message = message;
        this.isRead = isRead;
    }

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatImage> chatImages = new ArrayList<>();

    public void addChatImage(ChatImage chatImage) {
        this.chatImages.add(chatImage);
    }
}