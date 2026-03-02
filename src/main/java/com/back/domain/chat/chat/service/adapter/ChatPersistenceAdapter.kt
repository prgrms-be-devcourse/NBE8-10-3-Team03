package com.back.domain.chat.chat.service.adapter

import com.back.domain.chat.chat.dto.response.UnreadCountResponse
import com.back.domain.chat.chat.entity.Chat
import com.back.domain.chat.chat.entity.ChatRoom
import com.back.domain.chat.chat.entity.ChatRoomType
import com.back.domain.chat.chat.repository.ChatRepository
import com.back.domain.chat.chat.repository.ChatRoomRepository
import com.back.domain.chat.chat.service.port.ChatPersistencePort
import org.springframework.data.domain.PageRequest
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

    override fun markMessagesAsReadByIds(chatIds: List<Int>): Int =
        chatRepository.markMessagesAsReadByIds(chatIds)

    override fun findLatestChatsByMember(apiKey: String): List<Chat> =
        chatRepository.findLatestChatIdsByMember(apiKey)
            .let { chatIds ->
                if (chatIds.isEmpty()) emptyList() else chatRepository.findChatsWithRoomByIds(chatIds)
            }

    override fun findLatestChatSummariesByMember(apiKey: String, memberId: Int): List<ChatPersistencePort.ChatRoomLatestSummary> =
        chatRepository.findLatestChatSummariesByMember(apiKey, memberId)
            .map { summary ->
                ChatPersistencePort.ChatRoomLatestSummary(
                    roomId = summary.getRoomId(),
                    latestChatId = summary.getLatestChatId(),
                    unreadCount = (summary.getUnreadCount() ?: 0L).toInt(),
                )
            }

    override fun findChatsWithRoomsByIds(chatIds: List<Int>): List<Chat> =
        if (chatIds.isEmpty()) emptyList() else chatRepository.findChatsWithRoomByIds(chatIds)

    override fun countUnreadMessagesByRoomIds(roomIds: List<String>, memberId: Int): List<UnreadCountResponse> =
        chatRepository.countUnreadMessagesByRoomIds(roomIds, memberId)

    override fun countUnreadMessagesByRoomId(roomId: String, memberId: Int): Int =
        chatRepository.countUnreadMessagesByRoomId(roomId, memberId).toInt()

    override fun findRecentChats(roomId: String, lastChatId: Int?): List<Chat> =
        if (lastChatId == null || lastChatId <= 0) {
            val chatIds = chatRepository.findTopIdsByRoomId(roomId, TOP20)
            if (chatIds.isEmpty()) emptyList() else chatRepository.findChatsByIds(chatIds)
        } else {
            val chatIds = chatRepository.findTopIdsByRoomIdAndIdLessThan(roomId, lastChatId, TOP20)
            if (chatIds.isEmpty()) emptyList() else chatRepository.findChatsByIds(chatIds)
        }

    companion object {
        private val TOP20 = PageRequest.of(0, 20)
    }
}
