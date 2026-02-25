package com.back.domain.chat.chat.repository

import com.back.domain.chat.chat.entity.ChatRoom
import com.back.domain.chat.chat.entity.ChatRoomType
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRoomRepository : JpaRepository<ChatRoom, Int> {
    // UUID로 방 찾기 (삭제되지 않은 방만)
    fun findByRoomIdAndDeletedFalse(roomId: String): ChatRoom?

    // txType + itemId + buyer 기준으로 기존 방 조회
    fun findByTxTypeAndItemIdAndBuyerApiKeyAndDeletedFalse(
        txType: ChatRoomType,
        itemId: Int,
        buyerApiKey: String,
    ): ChatRoom?
}
