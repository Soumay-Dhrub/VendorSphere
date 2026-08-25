package com.vendorsphere.notification.dto;

import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.entity.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationEvent event,
        String title,
        String message,
        String entityType,
        UUID entityId,
        boolean read,
        Instant createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getEventType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getEntityType(),
                notification.getEntityId(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
