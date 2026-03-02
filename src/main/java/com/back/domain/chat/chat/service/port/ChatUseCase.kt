package com.back.domain.chat.chat.service.port

import com.back.domain.chat.chat.dto.request.ChatMessageRequest
import com.back.domain.chat.chat.dto.response.ChatIdResponse
import com.back.domain.chat.chat.dto.response.ChatResponse
import com.back.domain.chat.chat.dto.response.ChatRoomIdResponse
import com.back.domain.chat.chat.dto.response.ChatRoomListResponse
import com.back.global.rsData.RsData

// Inbound port: 웹 어댑터가 의존해야 하는 채팅 유스케이스 계약.
interface ChatUseCase {
    fun createChatRoom(itemId: Int, txType: String): RsData<ChatRoomIdResponse>
    fun saveMessage(req: ChatMessageRequest): RsData<ChatIdResponse>
    fun getMessages(roomId: String, lastChatId: Int?): RsData<List<ChatResponse>>
    fun getChatList(): RsData<List<ChatRoomListResponse>>
    fun exitChatRoom(roomId: String): RsData<Void?>
}
