package com.vendorsphere.audit.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * The optional filters of the audit log read API (Requirements 29.4, 29.5, 29.6). Every component
 * may be {@code null}, in which case the filter is not applied; supplying several narrows the
 * result to the rows satisfying all of them.
 *
 * <p>{@code from} and {@code to} bound the creation instant inclusively.
 *
 * <p>The organization is deliberately not a component: reads are always scoped to the caller's
 * organization by the service, so it cannot be widened from a request (Requirements 29.3, 30.10).
 */
public record AuditSearchCriteria(
        UUID actorId,
        String entityType,
        UUID entityId,
        Instant from,
        Instant to
) {

    /** No filters, so every audit log entry of the caller's organization qualifies. */
    public static AuditSearchCriteria none() {
        return new AuditSearchCriteria(null, null, null, null, null);
    }
}
