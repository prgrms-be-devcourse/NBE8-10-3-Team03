package com.back.domain.chat.chat.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ChatRoomTest {

    @Test
    @DisplayName("POST 채팅방 생성 시 거래 타입과 스냅샷이 설정된다.")
    fun t1() {
        val room = ChatRoom.createForPost(
            itemId = 10,
            itemName = "중고 노트북",
            itemPrice = 100000,
            itemImageUrl = "https://img/a.jpg",
            sellerApiKey = "seller-key",
            buyerApiKey = "buyer-key"
        )

        assertThat(room.txType).isEqualTo(ChatRoomType.POST)
        assertThat(room.itemId).isEqualTo(10)
        assertThat(room.sellerApiKey).isEqualTo("seller-key")
        assertThat(room.buyerApiKey).isEqualTo("buyer-key")
        assertThat(room.roomId).isNotBlank()
    }

    @Test
    @DisplayName("양측 모두 퇴장하면 isBothExited가 true가 된다.")
    fun t2() {
        val room = ChatRoom.createForAuction(
            itemId = 1,
            itemName = "경매 물품",
            itemPrice = 50000,
            itemImageUrl = null,
            sellerApiKey = "seller-key",
            buyerApiKey = "buyer-key"
        )

        room.exit("seller-key")
        assertThat(room.isBothExited).isFalse()

        room.exit("buyer-key")
        assertThat(room.isBothExited).isTrue()
    }

    @Test
    @DisplayName("softDelete 호출 시 삭제 플래그와 삭제 시간이 기록된다.")
    fun t3() {
        val room = ChatRoom.createForPost(
            itemId = 3,
            itemName = "물품",
            itemPrice = 3000,
            itemImageUrl = null,
            sellerApiKey = "seller-key",
            buyerApiKey = "buyer-key"
        )

        room.softDelete()

        assertThat(room.deleted).isTrue()
        assertThat(room.deletedAt).isNotNull()
    }
}
