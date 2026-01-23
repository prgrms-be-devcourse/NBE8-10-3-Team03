package com.back.global.audit.service;

import com.back.global.audit.entity.SecurityAuditLog;
import com.back.global.audit.enums.AuditType;
import com.back.global.audit.repository.SecurityAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SecurityAuditService {

    private final SecurityAuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Integer memberId, AuditType type, String ip, String ua) {
        repository.save(
                new SecurityAuditLog(memberId, type, ip, ua)
        );
    }
}
