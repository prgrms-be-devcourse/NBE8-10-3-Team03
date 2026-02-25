package com.back.global.audit.service

import com.back.global.audit.entity.SecurityAuditLog
import com.back.global.audit.enums.AuditType
import com.back.global.audit.repository.SecurityAuditLogRepository
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class SecurityAuditService(
    private val repository: SecurityAuditLogRepository
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun log(memberId: Int?, type: AuditType, ip: String?, ua: String?) {
        repository.save(
            SecurityAuditLog(memberId, type, ip, ua)
        )
    }
}
