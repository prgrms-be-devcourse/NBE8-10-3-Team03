package com.back.domain.member.review.dto

import com.back.domain.member.review.entity.Review
import java.time.LocalDateTime

@JvmRecord
data class ReviewDto(
    val id: Int,
    val createDate: LocalDateTime?,
    val modifyDate: LocalDateTime?,
    val score: Int,
    val comment: String?,
    val memberId: Int,
    val reviewerId: Int
) {
    constructor(review: Review) : this(
        review.getId(),
        review.getCreateDate(),
        review.getModifyDate(),
        review.getStar(),
        review.getComment(),
        review.getMember().getId(),
        review.getReviewerId()
    )
}
