package com.vendorsphere.notification.service;

import com.vendorsphere.common.dto.PageResponse;
import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.dto.NotificationResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    void createOnce(UUID recipientId, NotificationEvent event, String entityType, UUID entityId,
                    String title, String message);

    void createForRole(UUID organizationId, String roleName, NotificationEvent event,
                       String entityType, UUID entityId, String title, String message);

    void createForVendorUsers(UUID vendorId, NotificationEvent event, String entityType,
                              UUID entityId, String title, String message);

    PageResponse<NotificationResponse> list(boolean unreadOnly, Pageable pageable);

    void markRead(UUID id);

    void markAllRead();

    long unreadCount();
}
