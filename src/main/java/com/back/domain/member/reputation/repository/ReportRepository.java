package com.back.domain.member.reputation.repository;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.reputation.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ReportRepository extends JpaRepository<Report, Integer> {
    boolean existsByReporterAndTargetAndCreateDateAfter(
            Member reporter,
            Member target,
            LocalDateTime now);

    int countByReporterIdAndCreateDateAfter(int targetId, LocalDateTime now);
}
