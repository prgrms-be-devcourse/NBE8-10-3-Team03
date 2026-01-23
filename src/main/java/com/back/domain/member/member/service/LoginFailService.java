package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginFailService {

    private final MemberRepository memberRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(int memberId, LocalDateTime now) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow();

        member.increaseFailCount();
        member.updateLastFailAt(now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void lock(int memberId, LocalDateTime now) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow();

        member.lock();
        member.lockUntil(now.plusMinutes(10));
    }
}

