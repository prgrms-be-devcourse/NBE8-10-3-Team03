package com.back.domain.chat.chat.service.event

import com.back.domain.chat.chat.dto.response.ChatNotification
import com.back.domain.chat.chat.service.port.ChatMemberPort
import com.back.domain.chat.chat.service.port.ChatPersistencePort
import com.back.domain.chat.chat.service.port.ChatPublishPort
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ChatMessageCommittedEventHandler(
    private val chatPublishPort: ChatPublishPort,
    private val chatMemberPort: ChatMemberPort,
    private val chatPersistencePort: ChatPersistencePort,
) {
    @Async("chatTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: ChatMessageCommittedEvent) {
        runCatching {
            chatPublishPort.publishRoomMessage(event.roomId, event.roomMessagePayload)
            val opponentApiKey = if (event.sellerApiKey == event.senderApiKey) event.buyerApiKey else event.sellerApiKey
            val opponent = chatMemberPort.findMemberByApiKey(opponentApiKey) ?: return

            val unreadCount = chatPersistencePort.countUnreadMessagesByRoomId(event.roomId, opponent.id)
            val notification = ChatNotification(
                type = "NEW_MESSAGE",
                roomId = event.roomId,
                opponentId = event.senderId,
                opponentNickname = event.senderNickname,
                opponentProfileImageUrl = event.senderProfileImageUrl,
                lastMessage = event.message,
                lastMessageDate = event.messageDate,
                unreadCount = unreadCount,
                itemId = event.itemId,
                itemName = event.itemName,
                itemImageUrl = event.itemImageUrl,
                itemPrice = event.itemPrice,
                txType = event.txType,
            )
            chatPublishPort.publishUserNotification(opponent.id, notification)
        }.onFailure { e ->
            log.warn("메시지 후속 비동기 처리 실패 - RoomId: {}, Error: {}", event.roomId, e.message)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ChatMessageCommittedEventHandler::class.java)
    }
}
