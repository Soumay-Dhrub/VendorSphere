package com.vendorsphere.notification.repository;

import com.vendorsphere.notification.NotificationEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Writes notifications with a single conditional insert.
 *
 * <pre>{@code
 * INSERT INTO notifications (user_id, title, message, entity_type, entity_id, event_type, read, created_at)
 * VALUES (?, ?, ?, ?, ?, ?, FALSE, ?)
 * ON CONFLICT DO NOTHING
 * }</pre>
 *
 * <p>Two things matter here. First, {@code ON CONFLICT DO NOTHING} lets the partial unique index
 * {@code uq_notifications_event} on {@code (user_id, event_type, entity_type, entity_id)} decide
 * idempotence, so a second call for the same key leaves the existing row untouched and writes
 * nothing (Requirement 28.9). There is no read-then-write window, so two concurrent callers cannot
 * both conclude the notification is absent.
 *
 * <p>Second, the conflict is resolved inside the statement rather than raised and caught. Letting the
 * violation escape would produce a {@code DataIntegrityViolationException}, and by then PostgreSQL
 * has aborted the transaction and Spring has marked it rollback-only: catching the exception in Java
 * would not make the transaction usable again, so the caller's business change would fail on commit
 * because a notification happened to be a duplicate. Suppressing the conflict in the statement keeps
 * the caller's transaction clean, which is what makes it safe for notification writes to join it.
 *
 * <p>The statement runs on the {@link EntityManager}'s connection and opens no transaction of its
 * own, so the notification commits and rolls back with the business change.
 */
class NotificationInsertImpl implements NotificationInsert {

    private static final String INSERT_SQL =
            """
            INSERT INTO notifications
                (user_id, title, message, entity_type, entity_id, event_type, read, created_at)
            VALUES
                (:userId, :title, :message, :entityType, :entityId, :eventType, FALSE, :createdAt)
            ON CONFLICT DO NOTHING
            """;

    @PersistenceContext
    private EntityManager entityManager;

    private final Clock clock;

    NotificationInsertImpl(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean insertIfAbsent(
            UUID recipientId,
            NotificationEvent event,
            String entityType,
            UUID entityId,
            String title,
            String message) {
        int inserted = entityManager
                .createNativeQuery(INSERT_SQL)
                .setParameter("userId", recipientId)
                .setParameter("title", title)
                .setParameter("message", message)
                .setParameter("entityType", entityType)
                .setParameter("entityId", entityId)
                .setParameter("eventType", event.name())
                .setParameter("createdAt", Instant.now(clock))
                .executeUpdate();
        return inserted > 0;
    }
}
