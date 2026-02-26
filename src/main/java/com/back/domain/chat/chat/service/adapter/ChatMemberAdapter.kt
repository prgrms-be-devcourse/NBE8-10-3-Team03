package com.back.domain.chat.chat.service.adapter

import com.back.domain.chat.chat.service.port.ChatMemberInfo
import com.back.domain.chat.chat.service.port.ChatMemberPort
import com.back.domain.member.member.repository.MemberRepository
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Component

/**
 * ChatMemberPort 구현체.
 * Member 엔티티를 채팅 전용 읽기 모델(ChatMemberInfo)로 매핑한다.
 */
@Component
class ChatMemberAdapter(
    private val memberRepository: MemberRepository,
) : ChatMemberPort {
    /** 회원 ID로 회원을 조회한다. */
    override fun getMemberOrThrow(memberId: Int): ChatMemberInfo =
        memberRepository.findById(memberId)
            .map { it.toInfo() }
            .orElseThrow { ServiceException("404-1", "존재하지 않는 회원입니다.") }

    /** apiKey로 회원을 조회한다. */
    override fun findMemberByApiKey(apiKey: String): ChatMemberInfo? =
        memberRepository.findByApiKey(apiKey).orElse(null)?.toInfo()

    /** apiKey 집합으로 회원 목록을 조회해 맵으로 반환한다. */
    override fun findMembersByApiKeys(apiKeys: Set<String>): Map<String, ChatMemberInfo> =
        memberRepository.findByApiKeyIn(apiKeys.toMutableSet())
            .map { it.toInfo() }
            .associateBy { it.apiKey }

    private fun com.back.domain.member.member.entity.Member.toInfo(): ChatMemberInfo =
        ChatMemberInfo(
            id = id,
            nickname = nickname,
            profileImageUrl = profileImgUrl,
            apiKey = apiKey ?: throw ServiceException("500-1", "회원 apiKey가 없습니다."),
            reputationScore = reputation?.score,
        )
}
