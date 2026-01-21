package com.back.domain.member.member.entity;

import com.back.domain.member.member.enums.EventType;
import com.back.domain.member.member.enums.RefType;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "reputation_events")
public class ReputationEvent extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    private RefType refType;

    private int refId;

    private double delta;

    public ReputationEvent(Member member, EventType eventType, RefType refType) {
        this(member, eventType, refType, 0, 0);
    }

    public ReputationEvent(Member member, EventType eventType, RefType refType, int refId, double delta) {
        this.member = member;
        this.eventType = eventType;
        this.refType = refType;
        this.refId = refId;
        this.delta = delta;
    }
}
