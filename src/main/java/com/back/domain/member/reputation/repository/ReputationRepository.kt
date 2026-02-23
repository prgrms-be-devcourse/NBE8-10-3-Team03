package com.back.domain.member.reputation.repository

import com.back.domain.member.reputation.entity.Reputation
import org.springframework.data.jpa.repository.JpaRepository

interface ReputationRepository : JpaRepository<Reputation, Int>
