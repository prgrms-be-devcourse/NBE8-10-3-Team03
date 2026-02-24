package com.back.global.audit.repository

import com.back.global.audit.entity.SecurityAuditLog
import org.springframework.data.jpa.repository.JpaRepository

interface SecurityAuditLogRepository : JpaRepository<SecurityAuditLog, Int>
