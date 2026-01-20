package com.back.domain.chat.chat.entity;

import com.back.domain.image.image.entity.Image;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(chatImageId.class)
public class ChatImage {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private Image image;

    public ChatImage(Chat chat, Image image) {
        this.chat = chat;
        this.image = image;
    }

}
