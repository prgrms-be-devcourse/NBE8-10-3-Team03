package com.back.domain.chat.chat.service.event

data class ChatRoomReadEvent(
    val roomId: String,
    val readerId: Int,
    val updatedCount: Int,
)
