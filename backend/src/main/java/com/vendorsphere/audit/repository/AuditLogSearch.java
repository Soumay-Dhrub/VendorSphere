package com.vendorsphere.audit.repository;

import com.vendorsphere.audit.dto.AuditSearchCriteria;
import com.vendorsphere.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * The read half of the append-only audit contract: one filtered, paged query
 * (Requirements 29.3 through 29.6).
 *
 * <p>Implemented by {@link AuditLogSearchImpl} and mixed into {@link AuditLogRepository} as a
 * Spring Data repository fragment.
 */
public interface AuditLogSearch {

    /**
     * Returns the audit trail rows of {@code organizationId} matching every supplied filter.
     *
     * @param organizationId the caller's organization; never widened by request input
     * @param criteria       the optional actor, entity and creation-instant filters
     * @param pageable       page, size and sort, whose sort the caller supplies (the read API
     *                       defaults it to creation instant descending per Requirement 29.3)
     */
    Page<AuditLog> search(UUID organizationId, AuditSearchCriteria criteria, Pageable pageable);
}
