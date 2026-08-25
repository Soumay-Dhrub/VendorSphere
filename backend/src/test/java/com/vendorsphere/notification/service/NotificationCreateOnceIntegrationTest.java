package com.vendorsphere.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.entity.Notification;
import com.vendorsphere.notification.repository.NotificationRepository;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.organization.repository.OrganizationRepository;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class NotificationCreateOnceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String ENTITY_TYPE = "PURCHASE_REQUEST";

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    void repeatedCreateOnceKeepsTheExistingRowAndLeavesTheTransactionUsable() {
        UUID recipientId = newUser();
        UUID entityId = UUID.randomUUID();

        notificationService.createOnce(recipientId, NotificationEvent.PURCHASE_REQUEST_SUBMITTED,
                ENTITY_TYPE, entityId, "First title", "First message");
        notificationService.createOnce(recipientId, NotificationEvent.PURCHASE_REQUEST_SUBMITTED,
                ENTITY_TYPE, entityId, "Second title", "Second message");

        Page<Notification> stored =
                notificationRepository.findByUserId(recipientId, PageRequest.of(0, 10));
        assertThat(stored.getTotalElements()).isEqualTo(1);
        Notification only = stored.getContent().getFirst();
        assertThat(only.getTitle()).isEqualTo("First title");
        assertThat(only.getMessage()).isEqualTo("First message");
        assertThat(only.getEventType()).isEqualTo(NotificationEvent.PURCHASE_REQUEST_SUBMITTED);
        assertThat(only.getEntityId()).isEqualTo(entityId);
        assertThat(only.isRead()).isFalse();

        // The suppressed duplicate did not poison the transaction: further writes still commit here.
        notificationService.createOnce(recipientId, NotificationEvent.PURCHASE_REQUEST_SUBMITTED,
                ENTITY_TYPE, UUID.randomUUID(), "Another entity", "Another message");
        assertThat(notificationRepository.countByUserIdAndReadFalse(recipientId)).isEqualTo(2);
    }

    @Test
    @Transactional
    void createOnceDeduplicatesPerEventRatherThanPerEntity() {
        UUID recipientId = newUser();
        UUID entityId = UUID.randomUUID();

        notificationService.createOnce(recipientId, NotificationEvent.PURCHASE_REQUEST_SUBMITTED,
                ENTITY_TYPE, entityId, "Submitted", "A purchase request was submitted");
        notificationService.createOnce(recipientId, NotificationEvent.PURCHASE_REQUEST_APPROVED,
                ENTITY_TYPE, entityId, "Approved", "A purchase request was approved");

        assertThat(notificationRepository.countByUserIdAndReadFalse(recipientId)).isEqualTo(2);
    }

    private UUID newUser() {
        Organization organization = new Organization();
        organization.setName("Notification Test Org");
        organization.setSlug("notif-" + UUID.randomUUID());
        organizationRepository.saveAndFlush(organization);

        User user = new User();
        user.setOrganization(organization);
        user.setEmail("recipient-" + UUID.randomUUID() + "@demo-corp.com");
        user.setPasswordHash("hash");
        user.setFirstName("Test");
        user.setLastName("Recipient");
        return userRepository.saveAndFlush(user).getId();
    }
}
