package com.vendorsphere.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditSearchCriteria(
        UUID actorId,
        String entityType,
        UUID entityId,
        Instant from,
        Instant to
) {

    public static AuditSearchCriteria none() {
        return new AuditSearchCriteria(null, null, null, null, null);
    }
}
