package com.back.global.audit.entity

import com.back.global.audit.enums.AuditType
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Entity
class SecurityAuditLog(
    @field:Column(nullable = true)
    private var memberId: Int?,
    @field:Enumerated(EnumType.STRING)
    private val type: AuditType?,
    private val ipAddress: String?,
    private val userAgent: String?
) : BaseEntity()
