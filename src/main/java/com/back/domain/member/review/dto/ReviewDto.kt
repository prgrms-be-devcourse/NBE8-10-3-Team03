package com.back.domain.member.review.dto

import com.back.domain.member.review.entity.Review
import java.time.LocalDateTime

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
        review.id,
        review.createDate,
        review.modifyDate,
        review.star,
        review.comment,
        review.member.id,
        review.reviewerId
    )
}
