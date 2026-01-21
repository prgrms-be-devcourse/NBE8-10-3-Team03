package com.back.domain.member.reputation.entity;

import com.back.domain.member.member.entity.Member;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
public class Reputation extends BaseEntity {
    @OneToOne
    @JoinColumn(name = "member_id")
    private Member member;
    private double score;
    private int caution;

    public Reputation(Member member, double score) {
        this.member = member;
        this.score = score;
        this.caution = 0;
    }

    public void update(int caution) {
        this.caution = caution;
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
}
