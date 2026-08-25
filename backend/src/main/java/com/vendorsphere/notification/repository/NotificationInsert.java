package com.vendorsphere.notification.repository;

import com.vendorsphere.notification.NotificationEvent;
import java.util.UUID;

public interface NotificationInsert {

    boolean insertIfAbsent(
            UUID recipientId,
            NotificationEvent event,
            String entityType,
            UUID entityId,
            String title,
            String message);
}
