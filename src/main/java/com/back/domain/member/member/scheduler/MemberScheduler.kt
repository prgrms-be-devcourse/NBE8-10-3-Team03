package com.back.domain.member.member.scheduler

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.enums.MemberStatus
import com.back.domain.member.member.repository.MemberRepository
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.function.Consumer

@Component
class MemberScheduler(
    private val memberRepository: MemberRepository
) {

    @Scheduled(cron = "0 0 * * * *") // 매 정시
    @Transactional
    fun releaseSuspendedMembers() {
        val threshold = LocalDateTime.now().minusDays(7)

        val targets: MutableList<Member> =
            memberRepository.findByStatusAndSuspendAtBefore(MemberStatus.SUSPENDED, threshold)

        targets.forEach{ it.release() }
    }
}
