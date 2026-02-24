package com.back.domain.chat.chat.dto.response

import com.back.domain.chat.chat.entity.Chat
import com.back.domain.chat.chat.entity.ChatRoomType
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
        fun from(chat: Chat, senderProfileImageUrl: String? = null): ChatResponse {
            val room = chat.chatRoom

            val itemId = when (room.txType) {
                ChatRoomType.POST -> room.post?.id
                ChatRoomType.AUCTION -> room.auction?.id
            }

            return ChatResponse(
                id = chat.id,
                itemId = itemId,
                roomId = room.roomId,
                senderId = chat.senderId,
                senderProfileImageUrl = senderProfileImageUrl,
                message = chat.message,
                createDate = chat.createDate,
                imageUrls = chat.chatImages.map { it.image.url },
                read = chat.read,
            )
        }
    }
}
