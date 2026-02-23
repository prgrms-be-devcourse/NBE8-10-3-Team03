package com.back.domain.member.reputation.entity

import com.back.domain.member.member.entity.Member
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import lombok.Getter
import lombok.NoArgsConstructor
import lombok.Setter
import kotlin.math.min

@Getter
@Setter
@Entity
@Table(name = "reputation", indexes = [Index(name = "idx_reputation_member", columnList = "member_id", unique = true)])
@NoArgsConstructor
class Reputation(
    @field:JoinColumn(name = "member_id") @field:OneToOne private var member: Member?,
    private var score: Double
) : BaseEntity() {
    private var notifyCount = 0 // 신고 누적 (리셋o)
    private var totalNotifyCount = 0 // 신고 total 누적 (리셋x)

    // 10% 감소
    fun decrease() {
        val newScore = this.getScore() * 0.9
        this.score = if (newScore > 0) newScore else 0.0
    }

    // 10% 증가
    fun increase() {
        val newScore = this.score * 1.1
        this.score = min(newScore, 100.0)
    }

    fun increaseNotify() {
        this.notifyCount++
        this.totalNotifyCount++
    }
}
