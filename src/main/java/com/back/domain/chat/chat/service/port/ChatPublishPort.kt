package com.back.domain.chat.chat.service.port

/**
 * 채팅 도메인의 메시지 발행(웹소켓 등)을 추상화한 출력 포트.
 * ChatService는 발행 대상/채널 규약만 알고 전송 기술은 모른다.
 */
interface ChatPublishPort {
    /** 채팅방 메시지 토픽으로 이벤트를 발행한다. */
    fun publishRoomMessage(roomId: String, payload: Any)

    /** 채팅방 읽음 상태 토픽으로 이벤트를 발행한다. */
    fun publishRoomRead(roomId: String, payload: Any)

    /** 사용자 개인 알림 토픽으로 이벤트를 발행한다. */
    fun publishUserNotification(userId: Int, payload: Any)
}
