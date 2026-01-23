package com.back.domain.member.member.scheduler;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.enums.MemberStatus;
import com.back.domain.member.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberScheduler {

    private final MemberRepository memberRepository;

    @Scheduled(cron = "0 0 * * * *") // 매 정시
    @Transactional
    public void releaseSuspendedMembers() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);

        List<Member> targets = memberRepository.findByStatusAndSuspendAtBefore(MemberStatus.SUSPENDED, threshold);

        targets.forEach(Member::release);
    }
}
