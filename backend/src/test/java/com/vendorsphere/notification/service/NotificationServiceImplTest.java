package com.vendorsphere.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vendorsphere.auth.security.UserPrincipal;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.entity.Notification;
import com.vendorsphere.notification.repository.NotificationRepository;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.user.RoleName;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class NotificationServiceImplTest {

    private static final String ENTITY_TYPE = "PURCHASE_REQUEST";

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final VendorUserDirectory vendorUserDirectory = mock(VendorUserDirectory.class);

    private final NotificationServiceImpl service =
            new NotificationServiceImpl(notificationRepository, userRepository, vendorUserDirectory);

    private final UUID callerId = UUID.randomUUID();
    private final UUID organizationId = UUID.randomUUID();

    @BeforeEach
    void authenticateCaller() {
        Organization organization = new Organization();
        organization.setId(organizationId);

        User user = new User();
        user.setId(callerId);
        user.setOrganization(organization);
        user.setEmail("caller@demo-corp.com");
        user.setPasswordHash("hash");

        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void markingAnotherUsersNotificationReadIsNotFound() {
        UUID foreignNotificationId = UUID.randomUUID();
        when(notificationRepository.findByIdAndUserId(foreignNotificationId, callerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(foreignNotificationId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Notification not found")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markingOwnNotificationSetsTheReadFlag() {
        Notification notification = new Notification();
        notification.setRead(false);
        UUID id = UUID.randomUUID();
        when(notificationRepository.findByIdAndUserId(id, callerId)).thenReturn(Optional.of(notification));

        service.markRead(id);

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAllReadTargetsTheCallersNotificationsOnly() {
        service.markAllRead();

        verify(notificationRepository).markAllReadForUser(callerId);
    }

    @Test
    void unreadCountCountsTheCallersUnreadNotifications() {
        when(notificationRepository.countByUserIdAndReadFalse(callerId)).thenReturn(4L);

        assertThat(service.unreadCount()).isEqualTo(4L);
    }

    @Test
    void listDefaultsToNewestFirstAndReadsOnlyTheCallersNotifications() {
        Page<Notification> empty = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(notificationRepository.findByUserId(eq(callerId), any(Pageable.class))).thenReturn(empty);

        service.list(false, PageRequest.of(0, 20));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findByUserId(eq(callerId), pageable.capture());
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void listKeepsAnExplicitSort() {
        Page<Notification> empty = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        Sort explicit = Sort.by(Sort.Direction.ASC, "title");
        when(notificationRepository.findByUserId(eq(callerId), any(Pageable.class))).thenReturn(empty);

        service.list(false, PageRequest.of(0, 20, explicit));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findByUserId(eq(callerId), pageable.capture());
        assertThat(pageable.getValue().getSort()).isEqualTo(explicit);
    }

    @Test
    void listWithTheUnreadFilterReadsOnlyUnreadNotifications() {
        Page<Notification> empty = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(notificationRepository.findByUserIdAndReadFalse(eq(callerId), any(Pageable.class)))
                .thenReturn(empty);

        service.list(true, PageRequest.of(0, 20));

        verify(notificationRepository).findByUserIdAndReadFalse(eq(callerId), any(Pageable.class));
        verify(notificationRepository, never()).findByUserId(any(), any());
    }

    @Test
    void roleFanOutWritesOneNotificationPerRoleMember() {
        UUID firstManager = UUID.randomUUID();
        UUID secondManager = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        when(userRepository.findActiveUserIdsByOrganizationIdAndRoleName(
                organizationId, RoleName.PROCUREMENT_MANAGER))
                .thenReturn(List.of(firstManager, secondManager));

        service.createForRole(organizationId, RoleName.PROCUREMENT_MANAGER,
                NotificationEvent.PURCHASE_REQUEST_SUBMITTED, ENTITY_TYPE, entityId,
                "PR-2026-001 submitted", "A purchase request awaits review");

        verify(notificationRepository).insertIfAbsent(firstManager,
                NotificationEvent.PURCHASE_REQUEST_SUBMITTED, ENTITY_TYPE, entityId,
                "PR-2026-001 submitted", "A purchase request awaits review");
        verify(notificationRepository).insertIfAbsent(secondManager,
                NotificationEvent.PURCHASE_REQUEST_SUBMITTED, ENTITY_TYPE, entityId,
                "PR-2026-001 submitted", "A purchase request awaits review");
    }

    @Test
    void vendorFanOutWritesOneNotificationPerLinkedVendorUser() {
        UUID vendorId = UUID.randomUUID();
        UUID vendorUserId = UUID.randomUUID();
        UUID purchaseOrderId = UUID.randomUUID();
        when(vendorUserDirectory.findUserIdsOfVendor(vendorId)).thenReturn(List.of(vendorUserId));

        service.createForVendorUsers(vendorId, NotificationEvent.PURCHASE_ORDER_ISSUED,
                "PURCHASE_ORDER", purchaseOrderId, "PO-2026-001 issued", "A purchase order was issued");

        verify(notificationRepository).insertIfAbsent(vendorUserId,
                NotificationEvent.PURCHASE_ORDER_ISSUED, "PURCHASE_ORDER", purchaseOrderId,
                "PO-2026-001 issued", "A purchase order was issued");
    }

    @Test
    void vendorFanOutWithoutALinkedUserWritesNothing() {
        UUID vendorId = UUID.randomUUID();
        when(vendorUserDirectory.findUserIdsOfVendor(vendorId)).thenReturn(List.of());

        service.createForVendorUsers(vendorId, NotificationEvent.PURCHASE_ORDER_ISSUED,
                "PURCHASE_ORDER", UUID.randomUUID(), "PO-2026-001 issued", "A purchase order was issued");

        verify(notificationRepository, never())
                .insertIfAbsent(any(), any(), any(), any(), any(), any());
    }
}
