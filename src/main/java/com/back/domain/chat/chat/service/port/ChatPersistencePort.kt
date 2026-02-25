package com.back.domain.chat.chat.service.port

import com.back.domain.chat.chat.dto.response.UnreadCountResponse
import com.back.domain.chat.chat.entity.Chat
import com.back.domain.chat.chat.entity.ChatRoom
import com.back.domain.chat.chat.entity.ChatRoomType

/**
 * 채팅 도메인의 영속화 동작을 추상화한 출력 포트.
 * 서비스는 저장소 구현(JPA 등) 대신 이 포트에 의존한다.
 */
interface ChatPersistencePort {
    fun findActiveRoom(roomId: String): ChatRoom?

    fun findExistingRoom(txType: ChatRoomType, itemId: Int, buyerApiKey: String): ChatRoom?

    fun saveRoom(room: ChatRoom): ChatRoom

    fun saveChat(chat: Chat): Chat

    fun markMessagesAsRead(roomId: String, readerId: Int): Int

    fun findLatestChatsByMember(apiKey: String): List<Chat>

    fun countUnreadMessagesByRoomIds(roomIds: List<String>, memberId: Int): List<UnreadCountResponse>

    fun findRecentChats(roomId: String, lastChatId: Int?): List<Chat>
}

