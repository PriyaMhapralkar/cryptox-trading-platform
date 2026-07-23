package com.cryptox.backend.service;

import com.cryptox.backend.entity.*;
import com.cryptox.backend.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class AuditLogService {

    @Autowired private AuditLogRepository auditLogRepository;

    public void log(User user, String action, String status, String details) {
        AuditLog entry = AuditLog.builder()
                .user(user)
                .action(action)
                .status(status)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(entry);
    }
}