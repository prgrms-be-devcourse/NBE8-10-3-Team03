package com.back.domain.chat.chat.entity

import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Table(name = "chat")
@Entity
class Chat() : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", referencedColumnName = "roomId", nullable = false)
    lateinit var chatRoom: ChatRoom

    @Column(name = "sender_id", nullable = false)
    var senderId: Int = 0

    @Column(columnDefinition = "TEXT")
    var message: String? = null

    @Column(name = "is_read", nullable = false)
    var read: Boolean = false

    @OneToMany(mappedBy = "chat", cascade = [CascadeType.ALL], orphanRemoval = true)
    val chatImages: MutableList<ChatImage> = mutableListOf()

    constructor(
        chatRoom: ChatRoom,
        senderId: Int,
        message: String?,
        read: Boolean,
    ) : this() {
        this.chatRoom = chatRoom
        this.senderId = senderId
        this.message = message
        this.read = read
    }

    fun addChatImage(chatImage: ChatImage) {
        chatImages.add(chatImage)
    }
}
