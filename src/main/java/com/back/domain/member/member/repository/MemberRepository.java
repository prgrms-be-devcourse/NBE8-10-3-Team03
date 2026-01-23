package com.back.domain.member.member.repository;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MemberRepository extends JpaRepository<Member, Integer> {
    Optional<Member> findByUsername(String username);

    Optional<Member> findByApiKey(String apiKey);

    Optional<Member> findByNickname(String nickname);

    List<Member> findByApiKeyIn(Set<String> apiKeys);
    List<Member> findByStatusAndSuspendAtBefore(
            MemberStatus status,
            LocalDateTime time
    );

}

