package com.back.domain.chat.chat.service.event

import com.back.domain.chat.chat.dto.response.ChatResponse
import com.back.domain.chat.chat.entity.ChatRoomType
import java.time.LocalDateTime

data class ChatMessageCommittedEvent(
    val roomId: String,
    val senderId: Int,
    val senderApiKey: String,
    val senderNickname: String,
    val senderProfileImageUrl: String?,
    val txType: ChatRoomType,
    val itemId: Int?,
    val itemName: String?,
    val itemImageUrl: String?,
    val itemPrice: Int?,
    val sellerApiKey: String,
    val buyerApiKey: String,
    val message: String?,
    val messageDate: LocalDateTime?,
    val roomMessagePayload: ChatResponse,
)
