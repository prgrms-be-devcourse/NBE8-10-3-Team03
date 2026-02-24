package com.back.domain.chat.chat.dto.response

import com.back.domain.chat.chat.entity.ChatRoomType
import java.time.LocalDateTime

data class ChatRoomListResponse(
    val roomId: String?,
    val opponentId: Int?,
    val opponentNickname: String?,
    val opponentProfileImageUrl: String?,
    val opponentReputation: Double?,
    val lastMessage: String?,
    val lastMessageDate: LocalDateTime?,
    val unreadCount: Int?,
    val itemId: Int?,
    val itemName: String?,
    val itemImageUrl: String?,
    val itemPrice: Int?,
    val txType: ChatRoomType?,
)
