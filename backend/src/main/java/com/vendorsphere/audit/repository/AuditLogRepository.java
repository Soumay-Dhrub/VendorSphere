package com.vendorsphere.audit.repository;

import com.vendorsphere.audit.entity.AuditLog;
import org.springframework.data.repository.Repository;

import java.util.UUID;

/**
 * Append-only persistence contract for the audit trail (Requirement 29.8).
 *
 * <p>It extends the bare {@link Repository} marker rather than {@code JpaRepository} or
 * {@code CrudRepository} on purpose: those hand every caller {@code delete}, {@code deleteAll},
 * {@code deleteById} and {@code saveAndFlush}-driven merges, which would make append-only a matter
 * of convention. Here the only declared operations are one insert and the filtered read contributed
 * by {@link AuditLogSearch}, so removing or rewriting an entry does not compile.
 *
 * <p>{@link org.springframework.data.jpa.repository.JpaSpecificationExecutor} is avoided for the
 * same reason — it contributes {@code delete(Specification)} — which is why the filtered read is a
 * hand-written fragment instead.
 */
public interface AuditLogRepository extends Repository<AuditLog, UUID>, AuditLogSearch {

    /**
     * Inserts one audit trail row. Callers always pass a freshly constructed {@link AuditLog}, so
     * this is an insert; no read path hands out a managed instance that could be re-saved as an
     * update.
     */
    AuditLog save(AuditLog auditLog);
}
