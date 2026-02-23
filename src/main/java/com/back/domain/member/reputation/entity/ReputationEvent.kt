package com.back.domain.member.reputation.entity

import com.back.domain.member.member.entity.Member
import com.back.domain.member.reputation.enums.EventType
import com.back.domain.member.reputation.enums.RefType
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import lombok.Getter
import lombok.NoArgsConstructor

@Getter
@Entity
@NoArgsConstructor
@Table(name = "reputation_events")
class ReputationEvent(
    @field:JoinColumn(name = "target_id") @field:ManyToOne private var target: Member?,
    @field:Enumerated(
        EnumType.STRING
    ) private var eventType: EventType?,
    @field:Enumerated(EnumType.STRING) private var refType: RefType?,
    private var refId: Int,
    private var delta: Double
) : BaseEntity()
