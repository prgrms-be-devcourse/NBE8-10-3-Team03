package com.back.domain.chat.chat.entity

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.member.member.entity.Member
import com.back.domain.post.post.entity.Post
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
class ChatRoom (
    @Column(unique = true, nullable = false)
    var roomId: String = "",

    // 거래의 종류 (AUCTION or POST)
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var txType: ChatRoomType = ChatRoomType.POST,

    // 경매 상품일 경우 참조하고 일반 상품일 경우 NULL
    @JoinColumn(name = "auction_id")
    @ManyToOne(fetch = FetchType.LAZY)
    var auction: Auction? = null,

    // 일반 상품일 경우 참조하고 경매 상품일 경우 NULL
    @JoinColumn(name = "post_id")
    @ManyToOne(fetch = FetchType.LAZY)
    var post: Post? = null,

    @Column(name = "seller_api_key", nullable = false)
    var sellerApiKey: String = "",

    @Column(name = "buyer_api_key", nullable = false)
    var buyerApiKey: String = "",

    @Column(name = "seller_exited", nullable = false)
    var sellerExited: Boolean = false,

    @Column(name = "buyer_exited", nullable = false)
    var buyerExited: Boolean = false,

    @Column(name = "deleted", nullable = false)
    var deleted: Boolean = false,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,
) : BaseEntity() {

    // 퇴장
    fun exit(apiKey: String?) {
        if (sellerApiKey == apiKey) this.sellerExited = true
        if (buyerApiKey == apiKey) this.buyerExited = true
    }

    // 둘 다 나갔는지 확인
    val isBothExited: Boolean
        get() = sellerExited && buyerExited

    // 소프트 딜리트
    fun softDelete() {
        deleted = true
        deletedAt = LocalDateTime.now()
    }

    companion object {
        // 경매(Auction) 낙찰 후 채팅방 생성
        @JvmStatic
        fun createForAuction(auction: Auction, buyer: Member): ChatRoom {
            return ChatRoom(
                roomId = UUID.randomUUID().toString(),
                txType = ChatRoomType.AUCTION,
                auction = auction,
                post = null,
                sellerApiKey = auction.seller.apiKey,
                buyerApiKey = buyer.apiKey,
            )
        }

        // 일반 판매(Post)용 채팅방 생성
        @JvmStatic
        fun createForPost(post: Post, buyer: Member): ChatRoom {
            return ChatRoom(
                roomId = UUID.randomUUID().toString(),
                txType = ChatRoomType.POST,
                auction = null,
                post = post,
                sellerApiKey = buyer.apiKey,
                buyerApiKey = buyer.apiKey,
            )
        }
    }
}