package com.vendorsphere.audit.service;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.dto.AuditLogResponse;
import com.vendorsphere.audit.dto.AuditSearchCriteria;
import com.vendorsphere.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * The append-only audit trail (Requirement 29). Two operations, one write and one read, and no
 * update or delete (Requirement 29.8).
 */
public interface AuditService {

    /**
     * Records one audit trail row for a completed state change (Requirements 29.1, 29.2).
     *
     * <p>Runs inside the caller's transaction, so a failure here rolls the business change back and
     * the response is 500 (Requirement 29.10). Call it at the end of the state-changing service
     * method, after the business change is applied.
     *
     * <p>{@code previous} and {@code current} are serialized to JSON by
     * {@link AuditPayloadSerializer}, which redacts credential-shaped properties. Pass DTOs, records
     * or maps rather than JPA entities. Either may be {@code null}: a creation has no previous
     * state, a deletion no new state.
     *
     * @param action     the operation being recorded
     * @param entityType the affected record type, for example {@code Vendor}
     * @param entityId   the affected record identifier, {@code null} when the change is not tied to
     *                   a single row
     * @param previous   state before the change
     * @param current    state after the change
     */
    void record(AuditAction action, String entityType, UUID entityId, Object previous, Object current);

    /**
     * Returns the audit trail rows of the caller's organization matching {@code criteria}
     * (Requirements 29.3 through 29.6). Callers order by creation instant descending unless they ask
     * for something else.
     */
    PageResponse<AuditLogResponse> search(AuditSearchCriteria criteria, Pageable pageable);
}
