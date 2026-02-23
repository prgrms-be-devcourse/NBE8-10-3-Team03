package com.back.domain.member.reputation.repository

import com.back.domain.member.reputation.entity.ReputationEvent
import org.springframework.data.jpa.repository.JpaRepository

interface ReputationEventRepository : JpaRepository<ReputationEvent, Int>
