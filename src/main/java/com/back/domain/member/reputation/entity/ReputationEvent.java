package com.back.domain.member.reputation.entity;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.reputation.enums.EventType;
import com.back.domain.member.reputation.enums.RefType;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "reputation_events")
public class ReputationEvent extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "target_id")
    private Member target;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    private RefType refType;

    private int refId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = true)
    private Member reporter;

    private double delta;

    public ReputationEvent(Member target, EventType eventType, RefType refType, Member reporter) {
        this(target, eventType, refType, 0, 0, reporter);
    }

    public ReputationEvent(Member target, EventType eventType, RefType refType, int refId, double delta, Member reporter) {
        this.target = target;
        this.eventType = eventType;
        this.refType = refType;
        this.refId = refId;
        this.delta = delta;
        this.reporter = reporter;
    }
}
