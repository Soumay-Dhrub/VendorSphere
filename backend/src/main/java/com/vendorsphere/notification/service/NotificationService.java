package com.vendorsphere.notification.service;

import com.vendorsphere.common.dto.PageResponse;
import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.dto.NotificationResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * In-app notifications (Requirement 28). Notifications are delivered through the API only; nothing
 * here sends email, which is out of scope for the MVP (Requirement 28.10).
 *
 * <p>The three {@code create...} methods are called from business services and join the caller's
 * transaction, so notifications commit and roll back with the change that triggered them. Every read
 * and mutation method resolves the recipient from the security context, so a caller can only ever see
 * and change their own notifications.
 */
public interface NotificationService {

    /**
     * Creates one notification for one recipient, unless that recipient already holds one for the
     * same event, entity type and entity identifier, in which case the existing notification is left
     * unchanged and nothing is written (Requirements 28.1 and 28.9).
     *
     * <p>{@code entityType} and {@code entityId} are required: they are two of the four columns of
     * the dedupe key, and PostgreSQL treats NULLs in a unique index as distinct, so a notification
     * without them could not be deduplicated.
     */
    void createOnce(UUID recipientId, NotificationEvent event, String entityType, UUID entityId,
                    String title, String message);

    /**
     * Fans {@link #createOnce} out to every active user holding {@code roleName} in the organization
     * (Requirements 5.5, 8.4, 12.7, 20.8 and the other role-addressed events of 28.2).
     */
    void createForRole(UUID organizationId, String roleName, NotificationEvent event,
                       String entityType, UUID entityId, String title, String message);

    /**
     * Fans {@link #createOnce} out to every user linked to the vendor through {@code vendors.user_id}
     * (Requirements 10.4, 19.3, 24.8 and 25.9). A vendor without a linked user account notifies
     * nobody, which is not an error.
     */
    void createForVendorUsers(UUID vendorId, NotificationEvent event, String entityType,
                              UUID entityId, String title, String message);

    /**
     * The calling user's notifications, newest first (Requirement 28.3), optionally restricted to
     * unread ones (Requirement 28.4).
     */
    PageResponse<NotificationResponse> list(boolean unreadOnly, Pageable pageable);

    /**
     * Marks one of the calling user's notifications read (Requirement 28.5).
     *
     * @throws com.vendorsphere.common.exception.BusinessException 404 {@code Notification not found}
     *         when the notification is unknown or addressed to another user (Requirement 28.6)
     */
    void markRead(UUID id);

    /** Marks every notification of the calling user read (Requirement 28.7). */
    void markAllRead();

    /** The calling user's unread notification count (Requirement 28.8). */
    long unreadCount();
}
