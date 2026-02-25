package com.back.domain.chat.chat.service.port

/**
 * 채팅방 접근 권한 검증에 필요한 최소 정보 제공 포트.
 * STOMP 구독 인가 로직에서 채팅방 저장소 구현을 직접 참조하지 않도록 분리한다.
 */
interface ChatRoomAccessPort {
    fun getActiveRoomOrThrow(roomId: String): ChatRoomAccessInfo
}

data class ChatRoomAccessInfo(
    val roomId: String,
    val sellerApiKey: String,
    val buyerApiKey: String,
)

