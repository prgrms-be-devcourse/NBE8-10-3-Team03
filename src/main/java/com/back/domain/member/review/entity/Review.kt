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
    @field:ManyToOne var member: Member,
    var star: Int,
    var comment: String?,
    var reviewerId: Int
) : BaseEntity()
