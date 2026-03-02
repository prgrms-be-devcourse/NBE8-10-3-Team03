package com.back.domain.chat.chat.service.adapter

import com.back.domain.chat.chat.service.port.ChatMemberInfo
import com.back.domain.chat.chat.service.port.ChatMemberPort
import com.back.domain.member.member.repository.MemberRepository
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * ChatMemberPort 구현체.
 * Member 엔티티를 채팅 전용 읽기 모델(ChatMemberInfo)로 매핑한다.
 */
@Component
class ChatMemberAdapter(
    private val memberRepository: MemberRepository,
) : ChatMemberPort {
    private val memberByIdCache = ConcurrentHashMap<Int, CachedMember>()
    private val memberByApiKeyCache = ConcurrentHashMap<String, CachedMember>()

    /** 회원 ID로 회원을 조회한다. */
    override fun getMemberOrThrow(memberId: Int): ChatMemberInfo =
        getCachedMemberById(memberId)
            ?: memberRepository.findById(memberId)
                .map { it.toInfo() }
                .orElseThrow { ServiceException("404-1", "존재하지 않는 회원입니다.") }
                .also(::cacheMember)

    /** apiKey로 회원을 조회한다. */
    override fun findMemberByApiKey(apiKey: String): ChatMemberInfo? =
        getCachedMemberByApiKey(apiKey)
            ?: memberRepository.findByApiKey(apiKey).orElse(null)?.toInfo()?.also(::cacheMember)

    /** apiKey 집합으로 회원 목록을 조회해 맵으로 반환한다. */
    override fun findMembersByApiKeys(apiKeys: Set<String>): Map<String, ChatMemberInfo> =
        apiKeys.mapNotNull { key -> getCachedMemberByApiKey(key) }
            .associateBy { it.apiKey }
            .let { cached ->
                val missingApiKeys = apiKeys - cached.keys
                if (missingApiKeys.isEmpty()) return cached

                val fetched = memberRepository.findByApiKeyIn(missingApiKeys.toMutableSet())
                    .map { it.toInfo().also(::cacheMember) }
                    .associateBy { it.apiKey }

                cached + fetched
            }

    private fun getCachedMemberById(memberId: Int): ChatMemberInfo? {
        val now = System.currentTimeMillis()
        val cached = memberByIdCache[memberId] ?: return null
        if (cached.expiresAtMillis <= now) {
            memberByIdCache.remove(memberId)
            memberByApiKeyCache.remove(cached.member.apiKey)
            return null
        }
        return cached.member
    }

    private fun getCachedMemberByApiKey(apiKey: String): ChatMemberInfo? {
        val now = System.currentTimeMillis()
        val cached = memberByApiKeyCache[apiKey] ?: return null
        if (cached.expiresAtMillis <= now) {
            memberByApiKeyCache.remove(apiKey)
            memberByIdCache.remove(cached.member.id)
            return null
        }
        return cached.member
    }

    private fun cacheMember(member: ChatMemberInfo) {
        val cached = CachedMember(
            member = member,
            expiresAtMillis = System.currentTimeMillis() + MEMBER_CACHE_TTL_MILLIS,
        )
        memberByIdCache[member.id] = cached
        memberByApiKeyCache[member.apiKey] = cached
    }

    private fun com.back.domain.member.member.entity.Member.toInfo(): ChatMemberInfo =
        ChatMemberInfo(
            id = id,
            nickname = nickname,
            profileImageUrl = profileImgUrl,
            apiKey = apiKey ?: throw ServiceException("500-1", "회원 apiKey가 없습니다."),
            reputationScore = reputation?.score,
        )

    private data class CachedMember(
        val member: ChatMemberInfo,
        val expiresAtMillis: Long,
    )

    companion object {
        private const val MEMBER_CACHE_TTL_MILLIS = 60_000L
    }
}
