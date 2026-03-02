package com.back.domain.chat.chat.dto.response

import com.back.domain.chat.chat.entity.Chat
import java.time.LocalDateTime

data class ChatResponse(
    val id: Int,
    val itemId: Int?,
    val roomId: String,
    val senderId: Int,
    val senderProfileImageUrl: String?,
    val message: String?,
    val createDate: LocalDateTime?,
    val imageUrls: List<String>,
    val read: Boolean,
) {
    companion object {
        fun from(
            chat: Chat,
            roomId: String,
            itemId: Int?,
            senderProfileImageUrl: String? = null,
            readOverride: Boolean? = null,
        ): ChatResponse =
            ChatResponse(
                id = chat.id,
                itemId = itemId,
                roomId = roomId,
                senderId = chat.senderId,
                senderProfileImageUrl = senderProfileImageUrl,
                message = chat.message,
                createDate = chat.createDate,
                imageUrls = chat.chatImages.map { it.image.url },
                read = readOverride ?: chat.read,
            )

        fun from(
            chat: Chat,
            senderProfileImageUrl: String? = null,
            readOverride: Boolean? = null,
        ): ChatResponse {
            val room = chat.chatRoom

            return ChatResponse(
                id = chat.id,
                itemId = room.itemId,
                roomId = room.roomId,
                senderId = chat.senderId,
                senderProfileImageUrl = senderProfileImageUrl,
                message = chat.message,
                createDate = chat.createDate,
                imageUrls = chat.chatImages.map { it.image.url },
                read = readOverride ?: chat.read,
            )
        }
    }
}
