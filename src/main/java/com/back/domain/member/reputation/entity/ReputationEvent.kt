package com.back.domain.member.reputation.entity

import com.back.domain.member.member.entity.Member
import com.back.domain.member.reputation.enums.EventType
import com.back.domain.member.reputation.enums.RefType
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "reputation_events")
class ReputationEvent(
    @field:JoinColumn(name = "target_id") @field:ManyToOne var target: Member,
    @field:Enumerated(EnumType.STRING) var eventType: EventType?,
    @field:Enumerated(EnumType.STRING) var refType: RefType?,
    private var refId: Int,
    var delta: Double
) : BaseEntity()
