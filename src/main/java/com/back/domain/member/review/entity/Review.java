package com.back.domain.member.review.entity;

import com.back.domain.member.member.entity.Member;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Review extends BaseEntity {
    @ManyToOne
    private Member member;
    private String comment;
    private int star;
    private int reviewerId;

    public Review(Member member, int star, String comment, int reviewerId) {
        this.member = member;
        this.star = star;
        this.comment = comment;
        this.reviewerId = reviewerId;
    }
}
