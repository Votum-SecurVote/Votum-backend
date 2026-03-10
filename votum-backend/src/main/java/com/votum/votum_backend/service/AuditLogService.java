package com.votum.votum_backend.service;

import com.votum.votum_backend.model.AuditLog;
import com.votum.votum_backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void logAction(String action) {
        String username = "System";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            username = (String) authentication.getPrincipal();
        }

        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .user(username)
                .time(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }
}
