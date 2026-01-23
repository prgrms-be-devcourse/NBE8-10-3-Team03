package com.back.global.audit.entity;

import com.back.global.audit.enums.AuditType;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDateTime;

@Entity
public class SecurityAuditLog extends BaseEntity {

    @Column(nullable = true)
    private Integer memberId;

    @Enumerated(EnumType.STRING)
    private AuditType type; 

    private String ipAddress;

    private String userAgent;

    public SecurityAuditLog(Integer memberId, AuditType type, String ipAddress, String userAgent) {
        this.memberId = memberId;
        this.type = type;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }
}
