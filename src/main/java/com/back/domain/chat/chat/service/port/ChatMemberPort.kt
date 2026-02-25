package com.back.domain.chat.chat.service.port

/**
 * 채팅 도메인이 회원 정보를 조회하기 위한 출력 포트.
 * 회원 엔티티 직접 의존을 줄이고 필요한 형태로만 데이터를 전달한다.
 */
interface ChatMemberPort {
    /** 회원 ID 기준으로 회원 정보를 조회한다. 없으면 예외를 던진다. */
    fun getMemberOrThrow(memberId: Int): ChatMemberInfo

    /** apiKey 기준으로 회원 정보를 조회한다. */
    fun findMemberByApiKey(apiKey: String): ChatMemberInfo?

    /** 여러 apiKey를 한 번에 조회해 맵 형태로 반환한다. */
    fun findMembersByApiKeys(apiKeys: Set<String>): Map<String, ChatMemberInfo>
}

/** 채팅 유스케이스에서 사용하는 회원 요약 모델. */
data class ChatMemberInfo(
    val id: Int,
    val nickname: String,
    val profileImageUrl: String?,
    val apiKey: String,
    val reputationScore: Double?,
)
