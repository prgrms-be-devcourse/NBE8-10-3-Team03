package com.back.domain.chat.chat.service.adapter

import com.back.domain.chat.chat.service.port.ChatPublishPort
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

/**
 * ChatPublishPort 구현체.
 * 채팅 도메인 이벤트를 STOMP destination 규약에 맞춰 전송한다.
 */
@Component
class ChatPublishAdapter(
    private val messagingTemplate: SimpMessagingTemplate,
) : ChatPublishPort {
    /** 채팅방 메시지 토픽으로 전송한다. */
    override fun publishRoomMessage(roomId: String, payload: Any) {
        messagingTemplate.convertAndSend("/sub/v1/chat/room/$roomId", payload)
    }

    /** 채팅방 read 토픽으로 전송한다. */
    override fun publishRoomRead(roomId: String, payload: Any) {
        messagingTemplate.convertAndSend("/sub/v1/chat/room/$roomId/read", payload)
    }

    /** 사용자 개인 알림 토픽으로 전송한다. */
    override fun publishUserNotification(userId: Int, payload: Any) {
        messagingTemplate.convertAndSend("/sub/user/$userId/notification", payload)
    }
}
