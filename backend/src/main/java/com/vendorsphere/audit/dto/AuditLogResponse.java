package com.vendorsphere.audit.dto;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.entity.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID organizationId,
        UUID actorId,
        AuditAction action,
        String entityType,
        UUID entityId,
        String previousValue,
        String newValue,
        String ipAddress,
        String userAgent,
        Instant createdAt
) {

    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getOrganization() == null ? null : auditLog.getOrganization().getId(),
                auditLog.getActor() == null ? null : auditLog.getActor().getId(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getPreviousValue(),
                auditLog.getNewValue(),
                auditLog.getIpAddress(),
                auditLog.getUserAgent(),
                auditLog.getCreatedAt());
    }
}
