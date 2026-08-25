package com.vendorsphere.notification.repository;

import com.vendorsphere.notification.NotificationEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

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
