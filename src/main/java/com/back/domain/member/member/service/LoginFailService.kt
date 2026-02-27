package com.back.domain.member.member.service

import com.back.domain.member.member.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class LoginFailService(
    private val memberRepository: MemberRepository
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun record(memberId: Int, now: LocalDateTime?) {
        val member = memberRepository.findById(memberId).orElseThrow()

        member.increaseFailCount()
        member.updateLastFailAt(now)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun lock(memberId: Int, now: LocalDateTime) {
        val member = memberRepository.findById(memberId)
            .orElseThrow()

        member.lock()
        member.lockUntil(now.plusMinutes(10))
    }
}
