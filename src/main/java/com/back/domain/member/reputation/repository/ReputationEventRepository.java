package com.back.domain.member.member.repository;

import com.back.domain.member.member.entity.Reputation;
import com.back.domain.member.member.entity.ReputationEvent;
import com.back.domain.member.member.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReputationEventRepository extends JpaRepository<ReputationEvent, Integer> {
    @Query("SELECT COUNT(re.member.id) FROM ReputationEvent re WHERE re.member.id = :userId AND re.eventType = :eventType")
    int findByUserIdWithSum(@Param("userId") int userId, @Param("eventType") EventType eventType);
}
