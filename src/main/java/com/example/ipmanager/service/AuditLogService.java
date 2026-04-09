package com.example.ipmanager.service;

import com.example.ipmanager.model.AuditLog;

import java.util.List;

public interface AuditLogService {

    void log(String entityType, Long entityId, String action, String detail);

    List<AuditLog> findLatest();
}