package com.vendorsphere.notification.entity;

import com.vendorsphere.common.entity.CreatedOnlyEntity;
import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * One in-app notification addressed to a single user (Requirement 28.1).
 *
 * <p>The row carries no organization identifier: the recipient does, and every read and write is
 * keyed on the recipient, so a notification is reachable only by the user it is addressed to and
 * therefore only within that user's organization (Requirements 28.3 and 28.6).
 *
 * <p>The {@code notifications} table carries {@code created_at} but no {@code updated_at}, so this
 * entity extends {@link CreatedOnlyEntity}. Marking a notification read is the only mutation.
 */
@Entity
@Table(name = "notifications")
public class Notification extends CreatedOnlyEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    /**
     * Part of the dedupe key of Requirement 28.9. Nullable in the schema because V1 rows predate the
     * column; {@code NotificationService.createOnce} always writes one.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 60)
    private NotificationEvent eventType;

    @Column(name = "read", nullable = false)
    private boolean read = false;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public NotificationEvent getEventType() {
        return eventType;
    }

    public void setEventType(NotificationEvent eventType) {
        this.eventType = eventType;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
