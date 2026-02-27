package com.back.domain.member.reputation.entity

import com.back.domain.member.member.entity.Member
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
class Report(
    @field:JoinColumn(name = "target_id")
    @field:ManyToOne
    private val target: Member?,

    @field:JoinColumn(name = "reporter_id", nullable = true)
    @field:ManyToOne(fetch = FetchType.LAZY)
    private val reporter: Member?
) : BaseEntity()
