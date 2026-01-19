package com.back.domain.member.member.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
public class Reputation extends BaseEntity {
    @OneToOne
    @JoinColumn(name = "member_id")
    private Member member;
    private double score;

    public Reputation(Member member, double score) {
        this.member = member;
        this.score = score;
    }
}
