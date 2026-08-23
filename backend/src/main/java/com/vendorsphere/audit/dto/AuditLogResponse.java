package com.vendorsphere.audit.dto;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.entity.AuditLog;

import java.time.Instant;
import java.util.UUID;

/**
 * One audit trail row as returned by {@code GET /audit-logs} (Requirement 29.1 lists every field).
 *
 * <p>{@code previousValue} and {@code newValue} are the stored JSON documents as text. They are
 * passed through untouched so the reader sees exactly what was recorded; the redaction that keeps
 * credentials out of them happened at write time.
 */
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
