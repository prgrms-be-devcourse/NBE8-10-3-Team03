package com.back.domain.chat.chat.service.adapter

import com.back.domain.chat.chat.dto.response.UnreadCountResponse
import com.back.domain.chat.chat.entity.Chat
import com.back.domain.chat.chat.entity.ChatRoom
import com.back.domain.chat.chat.entity.ChatRoomType
import com.back.domain.chat.chat.repository.ChatRepository
import com.back.domain.chat.chat.repository.ChatRoomRepository
import com.back.domain.chat.chat.service.port.ChatPersistencePort
import org.springframework.stereotype.Component

/**
 * ChatPersistencePort 구현체.
 * 채팅 메시지/채팅방 저장소 접근을 한 곳으로 모아 서비스 계층의 저장소 직접 의존을 제거한다.
 */
@Component
class ChatPersistenceAdapter(
    private val chatRepository: ChatRepository,
    private val chatRoomRepository: ChatRoomRepository,
) : ChatPersistencePort {
    override fun findActiveRoom(roomId: String): ChatRoom? =
        chatRoomRepository.findByRoomIdAndDeletedFalse(roomId)

    override fun findExistingRoom(txType: ChatRoomType, itemId: Int, buyerApiKey: String): ChatRoom? =
        chatRoomRepository.findByTxTypeAndItemIdAndBuyerApiKeyAndDeletedFalse(txType, itemId, buyerApiKey)

    override fun saveRoom(room: ChatRoom): ChatRoom = chatRoomRepository.save(room)

    override fun saveChat(chat: Chat): Chat = chatRepository.save(chat)

    override fun markMessagesAsRead(roomId: String, readerId: Int): Int =
        chatRepository.markMessagesAsRead(roomId, readerId)

    override fun findLatestChatsByMember(apiKey: String): List<Chat> =
        chatRepository.findAllLatestChatsByMember(apiKey)

    override fun countUnreadMessagesByRoomIds(roomIds: List<String>, memberId: Int): List<UnreadCountResponse> =
        chatRepository.countUnreadMessagesByRoomIds(roomIds, memberId)

    override fun findRecentChats(roomId: String, lastChatId: Int?): List<Chat> =
        if (lastChatId == null || lastChatId <= 0) {
            chatRepository.findTop20ByChatRoom_RoomIdOrderByIdDesc(roomId)
        } else {
            chatRepository.findTop20ByChatRoom_RoomIdAndIdLessThanOrderByIdDesc(roomId, lastChatId)
        }
}

