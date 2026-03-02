package com.back.domain.chat.chat.entity

import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(
    name = "chat_room",
    indexes = [
        Index(name = "idx_chat_room_tx_item_buyer_deleted", columnList = "tx_type, item_id, buyer_api_key, deleted"),
        Index(name = "idx_chat_room_seller_active", columnList = "seller_api_key, seller_exited, deleted"),
        Index(name = "idx_chat_room_buyer_active", columnList = "buyer_api_key, buyer_exited, deleted"),
    ],
)
class ChatRoom (
    @Column(unique = true, nullable = false)
    var roomId: String = "",

    // 거래의 종류 (AUCTION or POST)
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var txType: ChatRoomType = ChatRoomType.POST,

    // 거래 아이템 식별자(POST/AUCTION 공통)
    @Column(name = "item_id")
    var itemId: Int? = null,

    // 채팅방 생성 시점 아이템 스냅샷
    @Column(name = "item_name")
    var itemName: String? = null,

    @Column(name = "item_price")
    var itemPrice: Int? = null,

    @Column(name = "item_image_url")
    var itemImageUrl: String? = null,

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
    fun exit(apiKey: String) {
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
        fun createForAuction(
            itemId: Int,
            itemName: String?,
            itemPrice: Int?,
            itemImageUrl: String?,
            sellerApiKey: String,
            buyerApiKey: String,
        ): ChatRoom =
            ChatRoom(
                roomId = UUID.randomUUID().toString(),
                txType = ChatRoomType.AUCTION,
                itemId = itemId,
                itemName = itemName,
                itemPrice = itemPrice,
                itemImageUrl = itemImageUrl,
                sellerApiKey = sellerApiKey,
                buyerApiKey = buyerApiKey,
            )

        fun createForPost(
            itemId: Int,
            itemName: String?,
            itemPrice: Int?,
            itemImageUrl: String?,
            sellerApiKey: String,
            buyerApiKey: String,
        ): ChatRoom =
            ChatRoom(
                roomId = UUID.randomUUID().toString(),
                txType = ChatRoomType.POST,
                itemId = itemId,
                itemName = itemName,
                itemPrice = itemPrice,
                itemImageUrl = itemImageUrl,
                sellerApiKey = sellerApiKey,
                buyerApiKey = buyerApiKey,
            )
    }
}
