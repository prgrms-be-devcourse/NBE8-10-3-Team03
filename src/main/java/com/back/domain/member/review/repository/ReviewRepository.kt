package com.back.domain.member.review.repository

import com.back.domain.member.review.entity.Review
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewRepository : JpaRepository<Review, Int>
