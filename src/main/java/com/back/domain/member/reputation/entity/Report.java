package com.back.domain.member.reputation.entity;

import com.back.domain.member.member.entity.Member;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"reporter_id", "target_id", "create_date"}
                )
        }
)
public class Report extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "target_id")
    private Member target;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = true)
    private Member reporter;


    public Report(Member target, Member reporter) {
        this.target = target;
        this.reporter = reporter;
    }
}
