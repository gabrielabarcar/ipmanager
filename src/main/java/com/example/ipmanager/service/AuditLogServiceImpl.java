package com.example.ipmanager.service;

import com.example.ipmanager.model.AuditLog;
import com.example.ipmanager.repository.AuditLogRepository;
import com.example.ipmanager.util.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void log(String entityType, Long entityId, String action, String detail) {
        auditLogRepository.save(
                new AuditLog(
                        entityType,
                        entityId != null ? entityId.toString() : null,
                        action,
                        SecurityUtils.currentUsername(),
                        detail
                )
        );
    }

    @Override
    public List<AuditLog> findLatest() {
        return auditLogRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(50)
                .toList();
    }
}