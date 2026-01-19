package com.back.domain.member.member.repository;

import com.back.domain.member.member.entity.Reputation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReputationRepository extends JpaRepository<Reputation, Integer> {
}
