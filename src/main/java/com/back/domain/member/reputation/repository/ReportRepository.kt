package com.back.domain.member.reputation.repository

import com.back.domain.member.member.entity.Member
import com.back.domain.member.reputation.entity.Report
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface ReportRepository : JpaRepository<Report, Int> {
    fun existsByReporterAndTargetAndCreateDateAfter(
        reporter: Member,
        target: Member,
        now: LocalDateTime
    ): Boolean

    fun countByReporterIdAndCreateDateAfter(targetId: Int, now: LocalDateTime): Int
}
