package com.back.domain.chat.chat.entity

import com.back.domain.image.image.entity.Image
import jakarta.persistence.*

@Entity
@Table(name = "chat_images")
@IdClass(ChatImageId::class)
class ChatImage() {
    // chat, image 복합키
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    lateinit var chat: Chat

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    lateinit var image: Image

    constructor(chat: Chat, image: Image) : this() {
        this.chat = chat
        this.image = image
    }
}
