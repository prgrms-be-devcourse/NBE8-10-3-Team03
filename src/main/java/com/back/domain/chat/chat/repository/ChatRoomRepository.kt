package com.back.domain.chat.chat.repository

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.chat.chat.entity.ChatRoom
import com.back.domain.post.post.entity.Post
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRoomRepository : JpaRepository<ChatRoom, Int> {
    // UUID로 방 찾기 (삭제되지 않은 방만)
    fun findByRoomIdAndDeletedFalse(roomId: String): ChatRoom?

    // POST: 이미 존재하는 방인지 확인 (삭제되지 않은 방만)
    fun findByPostAndBuyerApiKeyAndDeletedFalse(post: Post, buyerApiKey: String): ChatRoom?

    // Auction: 이미 존재하는 방인지 확인 (삭제되지 않은 방만)
    fun findByAuctionAndBuyerApiKeyAndDeletedFalse(auction: Auction, buyerApiKey: String): ChatRoom?
}
