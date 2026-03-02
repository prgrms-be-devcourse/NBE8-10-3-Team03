package com.back.domain.chat.chat.service.event

import com.back.domain.chat.chat.service.port.ChatPublishPort
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ChatRoomReadEventHandler(
    private val chatPublishPort: ChatPublishPort,
) {
    @Async("chatTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: ChatRoomReadEvent) {
        val readNotification: Any = mapOf(
            "readerId" to event.readerId,
            "roomId" to event.roomId,
        )

        runCatching { chatPublishPort.publishRoomRead(event.roomId, readNotification) }
            .onSuccess {
                log.debug(
                    "읽음 알림 비동기 전송 - RoomId: {}, ReaderId: {}, UpdatedCount: {}",
                    event.roomId,
                    event.readerId,
                    event.updatedCount,
                )
            }
            .onFailure { e ->
                log.warn(
                    "읽음 알림 비동기 전송 실패 - RoomId: {}, ReaderId: {}, Error: {}",
                    event.roomId,
                    event.readerId,
                    e.message,
                )
            }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ChatRoomReadEventHandler::class.java)
    }
}
