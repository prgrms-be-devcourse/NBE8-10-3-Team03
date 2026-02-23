package com.back.domain.member.reputation.entity;

import com.back.domain.member.member.entity.Member;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "reputation", indexes = {
        // UNIQUE 인덱스: 1:1 관계 보장 및 JOIN 성능 최적화
        @Index(name = "idx_reputation_member", columnList = "member_id", unique = true)
})
@NoArgsConstructor
public class Reputation extends BaseEntity {
    @OneToOne
    @JoinColumn(name = "member_id")
    private Member member;
    private double score;
    private int notifyCount; // 신고 누적 (리셋o)
    private int totalNotifyCount;  // 신고 total 누적 (리셋x)

    public Reputation(Member member, double score) {
        this.member = member;
        this.score = score;
        this.notifyCount = 0;
        this.totalNotifyCount = 0;
    }

    // 10% 감소
    public void decrease() {
        double newScore = this.getScore() * 0.9;
        this.score = newScore > 0 ? newScore : 0;
    }

    // 10% 증가
    public void increase() {
        double newScore = this.score * 1.1;
        this.score = Math.min(newScore, 100);
    }

    public void increaseNotify() {
        this.notifyCount++;
        this.totalNotifyCount++;
    }

    public double getScore() {
        return score;
    }

    public int getNotifyCount() {
        return notifyCount;
    }

    public void setNotifyCount(int notifyCount) {
        this.notifyCount = notifyCount;
    }

    public int getTotalNotifyCount() {
        return totalNotifyCount;
    }
}
