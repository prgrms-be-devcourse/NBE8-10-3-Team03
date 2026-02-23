package com.back.domain.member.member.repository

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.enums.MemberStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.*

interface MemberRepository : JpaRepository<Member, Int> {
    fun findByUsername(username: String): Optional<Member>

    fun findByApiKey(apiKey: String): Optional<Member>

    fun findByNickname(nickname: String): Optional<Member>

    fun findByApiKeyIn(apiKeys: MutableSet<String>): MutableList<Member>
    fun findByStatusAndSuspendAtBefore(
        status: MemberStatus,
        time: LocalDateTime
    ): MutableList<Member>
}

