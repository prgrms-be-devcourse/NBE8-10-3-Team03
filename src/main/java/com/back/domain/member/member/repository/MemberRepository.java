package com.back.domain.member.member.repository;

import com.back.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Integer> {
    Optional<Member> findByUsername(String username);

    Optional<Member> findByApiKey(String apiKey);

    List<Member> findByActiveFalseAndSuspendAtBefore(LocalDateTime time);
}

