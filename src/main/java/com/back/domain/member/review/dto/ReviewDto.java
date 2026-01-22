package com.back.domain.member.review.dto;

import com.back.domain.member.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewDto(
        int id,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        int score,
        String comment,
        int memberId,
        int reviewerId
) {
    public ReviewDto(Review review) {
        this(
                review.getId(),
                review.getCreateDate(),
                review.getModifyDate(),
                review.getStar(),
                review.getComment(),
                review.getMember().getId(),
                review.getReviewerId()
        );
    }
}
