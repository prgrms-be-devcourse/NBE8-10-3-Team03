package com.back.domain.chat.chat.service.adapter

import com.back.domain.chat.chat.repository.ChatRoomRepository
import com.back.domain.chat.chat.service.port.ChatRoomAccessInfo
import com.back.domain.chat.chat.service.port.ChatRoomAccessPort
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Component

/**
 * ChatRoomAccessPort 구현체.
 * 채팅방 접근 검증에 필요한 필드만 추출해 반환한다.
 */
@Component
class ChatRoomAccessAdapter(
    private val chatRoomRepository: ChatRoomRepository,
) : ChatRoomAccessPort {
    override fun getActiveRoomOrThrow(roomId: String): ChatRoomAccessInfo {
        val room = chatRoomRepository.findByRoomIdAndDeletedFalse(roomId)
            ?: throw ServiceException("404-1", "존재하지 않는 채팅방입니다.")

        return ChatRoomAccessInfo(
            roomId = room.roomId,
            sellerApiKey = room.sellerApiKey,
            buyerApiKey = room.buyerApiKey,
        )
    }
}

