package com.vendorsphere.notification.service;

import com.vendorsphere.common.dto.PageResponse;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.common.util.PageSupport;
import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.dto.NotificationResponse;
import com.vendorsphere.notification.entity.Notification;
import com.vendorsphere.notification.repository.NotificationRepository;
import com.vendorsphere.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {

    /** Requirement 28.6: pinned verbatim. */
    static final String NOT_FOUND_MESSAGE = "Notification not found";

    /** Requirement 28.3: newest first, applied whenever the caller supplies no sort. */
    static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final VendorUserDirectory vendorUserDirectory;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            VendorUserDirectory vendorUserDirectory
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.vendorUserDirectory = vendorUserDirectory;
    }

    /**
     * No {@code propagation} override on purpose: the write joins the caller's business transaction,
     * so a rolled-back business change takes its notifications with it. That only works because the
     * insert resolves duplicates inside the statement and therefore cannot mark the transaction
     * rollback-only - see {@code NotificationInsertImpl}.
     */
    @Override
    @Transactional
    public void createOnce(UUID recipientId, NotificationEvent event, String entityType, UUID entityId,
                           String title, String message) {
        Objects.requireNonNull(recipientId, "recipientId must not be null");
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(entityType, "entityType must not be null: it is part of the dedupe key");
        Objects.requireNonNull(entityId, "entityId must not be null: it is part of the dedupe key");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(message, "message must not be null");

        notificationRepository.insertIfAbsent(recipientId, event, entityType, entityId, title, message);
    }

    @Override
    @Transactional
    public void createForRole(UUID organizationId, String roleName, NotificationEvent event,
                              String entityType, UUID entityId, String title, String message) {
        Objects.requireNonNull(organizationId, "organizationId must not be null");
        Objects.requireNonNull(roleName, "roleName must not be null");

        List<UUID> recipients =
                userRepository.findActiveUserIdsByOrganizationIdAndRoleName(organizationId, roleName);
        fanOut(recipients, event, entityType, entityId, title, message);
    }

    @Override
    @Transactional
    public void createForVendorUsers(UUID vendorId, NotificationEvent event, String entityType,
                                     UUID entityId, String title, String message) {
        Objects.requireNonNull(vendorId, "vendorId must not be null");

        fanOut(vendorUserDirectory.findUserIdsOfVendor(vendorId),
                event, entityType, entityId, title, message);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(boolean unreadOnly, Pageable pageable) {
        UUID recipientId = SecurityUtils.getCurrentUserId();
        Pageable resolved = newestFirstWhenUnsorted(pageable);

        Page<Notification> page = unreadOnly
                ? notificationRepository.findByUserIdAndReadFalse(recipientId, resolved)
                : notificationRepository.findByUserId(recipientId, resolved);

        return PageSupport.map(page, NotificationResponse::from);
    }

    @Override
    @Transactional
    public void markRead(UUID id) {
        UUID recipientId = SecurityUtils.getCurrentUserId();
        Notification notification = notificationRepository.findByIdAndUserId(id, recipientId)
                .orElseThrow(() -> new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllRead() {
        notificationRepository.markAllReadForUser(SecurityUtils.getCurrentUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByUserIdAndReadFalse(SecurityUtils.getCurrentUserId());
    }

    private void fanOut(List<UUID> recipients, NotificationEvent event, String entityType,
                        UUID entityId, String title, String message) {
        for (UUID recipientId : recipients) {
            createOnce(recipientId, event, entityType, entityId, title, message);
        }
    }

    /**
     * Requirement 28.3: creation instant descending is the default order. A caller that supplies its
     * own sort keeps it; a caller that supplies none, including any internal caller passing a bare
     * {@code PageRequest}, still gets newest first.
     */
    private Pageable newestFirstWhenUnsorted(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(PageSupport.DEFAULT_PAGE, PageSupport.DEFAULT_SIZE, NEWEST_FIRST);
        }
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
    }
}
