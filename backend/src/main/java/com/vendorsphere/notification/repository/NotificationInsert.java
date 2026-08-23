package com.vendorsphere.notification.repository;

import com.vendorsphere.notification.NotificationEvent;
import java.util.UUID;

/**
 * Idempotent insert fragment of {@link NotificationRepository} (Requirement 28.9).
 *
 * <p>Kept as a fragment rather than {@code save(...)} because the insert has to tolerate a duplicate
 * without failing, which JPA cannot express: a unique-constraint violation surfaces as
 * {@code DataIntegrityViolationException} and marks the surrounding transaction rollback-only, which
 * would poison the business transaction the notification write joins.
 */
public interface NotificationInsert {

    /**
     * Inserts one notification unless the recipient already holds one for the same event type,
     * entity type and entity identifier.
     *
     * <p>Runs on the caller's transaction and connection, so the notification commits and rolls back
     * with the business change that triggered it.
     *
     * @return {@code true} when a row was written, {@code false} when the duplicate was suppressed
     */
    boolean insertIfAbsent(
            UUID recipientId,
            NotificationEvent event,
            String entityType,
            UUID entityId,
            String title,
            String message);
}
