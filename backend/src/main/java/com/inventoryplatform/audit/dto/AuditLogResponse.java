package com.inventoryplatform.audit.dto;

import com.inventoryplatform.audit.AuditLog;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Long userId,
        String action,
        String entity,
        String entityId,
        String oldValue,
        String newValue,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getUserId(),
                log.getAction(),
                log.getEntity(),
                log.getEntityId(),
                log.getOldValue(),
                log.getNewValue(),
                log.getCreatedAt()
        );
    }
}
