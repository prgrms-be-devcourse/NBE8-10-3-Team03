package com.back.domain.chat.chat.service

import com.back.domain.chat.chat.service.port.ChatItemInfo
import com.back.domain.chat.chat.service.port.ChatItemPort
import com.back.domain.chat.chat.service.port.ChatMemberInfo
import com.back.domain.chat.chat.service.port.ChatMemberPort
import com.back.domain.chat.chat.service.port.ChatMediaPort
import com.back.domain.chat.chat.service.port.ChatPersistencePort
import com.back.domain.chat.chat.service.port.ChatPublishPort
import com.back.domain.member.member.entity.Member
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import org.springframework.context.ApplicationEventPublisher
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class ChatServiceTest {
    private val chatPersistencePort: ChatPersistencePort = mock(ChatPersistencePort::class.java) { invocation ->
        when (invocation.method.name) {
            "saveRoom" -> invocation.arguments[0]
            "countUnreadMessagesByRoomIds" -> emptyList<Any>()
            else -> Answers.RETURNS_DEFAULTS.answer(invocation)
        }
    }
    private val chatItemPort: ChatItemPort = mock(ChatItemPort::class.java)
    private val chatMemberPort: ChatMemberPort = mock(ChatMemberPort::class.java)
    private val chatMediaPort: ChatMediaPort = mock(ChatMediaPort::class.java)
    private val chatPublishPort: ChatPublishPort = mock(ChatPublishPort::class.java)
    private val rq: Rq = mock(Rq::class.java)
    private val eventPublisher: ApplicationEventPublisher = mock(ApplicationEventPublisher::class.java)

    private val chatService = ChatService(
        chatPersistencePort,
        chatItemPort,
        chatMemberPort,
        chatMediaPort,
        chatPublishPort,
        rq,
        eventPublisher,
    )

    @Test
    @DisplayName("잘못된 거래 타입이면 채팅방 생성이 실패한다.")
    fun t1() {
        assertThatThrownBy { chatService.createChatRoom(1, "invalid") }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("400-2")
    }

    @Test
    @DisplayName("본인 물품에는 채팅방을 생성할 수 없다.")
    fun t2() {
        `when`(rq.actor).thenReturn(Member(1, "buyer", "구매자"))
        `when`(chatMemberPort.getMemberOrThrow(1)).thenReturn(
            ChatMemberInfo(
                id = 1,
                nickname = "구매자",
                profileImageUrl = null,
                apiKey = "same-key",
                reputationScore = 50.0
            )
        )
        `when`(chatItemPort.getPostItemOrThrow(10)).thenReturn(
            ChatItemInfo(
                itemId = 10,
                itemName = "상품",
                itemPrice = 1000,
                itemImageUrl = null,
                sellerApiKey = "same-key"
            )
        )

        assertThatThrownBy { chatService.createChatRoom(10, "post") }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("400-3")
    }

    @Test
    @DisplayName("기존 채팅방이 있으면 재사용하고 새 알림은 보내지 않는다.")
    fun t3() {
        val buyer = ChatMemberInfo(1, "구매자", null, "buyer-key", 50.0)
        val item = ChatItemInfo(10, "상품", 1000, null, "seller-key")
        val existingRoom = com.back.domain.chat.chat.entity.ChatRoom.createForPost(
            itemId = 10,
            itemName = "상품",
            itemPrice = 1000,
            itemImageUrl = null,
            sellerApiKey = "seller-key",
            buyerApiKey = "buyer-key"
        )

        `when`(rq.actor).thenReturn(Member(1, "buyer", "구매자"))
        `when`(chatMemberPort.getMemberOrThrow(1)).thenReturn(buyer)
        `when`(chatItemPort.getPostItemOrThrow(10)).thenReturn(item)
        `when`(chatPersistencePort.findExistingRoom(com.back.domain.chat.chat.entity.ChatRoomType.POST, 10, "buyer-key"))
            .thenReturn(existingRoom)

        val result = chatService.createChatRoom(10, "post")

        assertThat(result.resultCode).isEqualTo("200-1")
        assertThat(result.data!!.roomId).isEqualTo(existingRoom.roomId)

        val userNotifications = Mockito.mockingDetails(chatPublishPort).invocations
            .count { it.method.name == "publishUserNotification" }
        assertThat(userNotifications).isZero()
    }

    @Test
    @DisplayName("신규 채팅방 생성 시 판매자에게 NEW_ROOM 알림을 발송한다.")
    fun t4() {
        val buyer = ChatMemberInfo(1, "구매자", null, "buyer-key", 50.0)
        val seller = ChatMemberInfo(2, "판매자", null, "seller-key", 70.0)
        val item = ChatItemInfo(20, "상품2", 2000, null, "seller-key")

        `when`(rq.actor).thenReturn(Member(1, "buyer", "구매자"))
        `when`(chatMemberPort.getMemberOrThrow(1)).thenReturn(buyer)
        `when`(chatItemPort.getPostItemOrThrow(20)).thenReturn(item)
        `when`(chatPersistencePort.findExistingRoom(com.back.domain.chat.chat.entity.ChatRoomType.POST, 20, "buyer-key"))
            .thenReturn(null)
        `when`(chatMemberPort.findMemberByApiKey("seller-key")).thenReturn(seller)

        val result = chatService.createChatRoom(20, "post")

        assertThat(result.resultCode).isEqualTo("200-1")
        assertThat(result.data!!.roomId).isNotBlank()

        val userNotifications = Mockito.mockingDetails(chatPublishPort).invocations
            .filter { it.method.name == "publishUserNotification" }
        assertThat(userNotifications).hasSize(1)
        assertThat(userNotifications[0].arguments[0]).isEqualTo(2)
    }
}
