package com.back.domain.member.review.entity

import com.back.domain.member.member.entity.Member
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import lombok.Getter
import lombok.NoArgsConstructor

@Entity
@Getter
@NoArgsConstructor
class Review(
    @field:ManyToOne private var member: Member?,
    private var star: Int,
    private var comment: String?,
    private var reviewerId: Int
) : BaseEntity()
