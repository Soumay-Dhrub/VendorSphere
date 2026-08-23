package com.vendorsphere.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vendorsphere.auth.security.CustomUserDetailsService;
import com.vendorsphere.auth.security.UserPrincipal;
import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.entity.Notification;
import com.vendorsphere.notification.repository.NotificationRepository;
import com.vendorsphere.notification.service.NotificationService;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.organization.repository.OrganizationRepository;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Requirements 28.6, 28.9 and 36.4 through the HTTP API rather than through the service.
 *
 * <p>The ownership rule is only worth as much as the wire behaviour it produces: the caller has to be
 * resolved from the real security context, the lookup has to miss, and {@code GlobalExceptionHandler}
 * has to render the pinned wording. A unit test with a stubbed repository can show none of that, so
 * every case here goes through {@link MockMvc} with the security filter chain in place against a real
 * PostgreSQL schema.
 *
 * <p>No {@code @Transactional} anywhere in this class, deliberately. Each request and each
 * {@code createOnce} call commits on its own, which is what makes the dedupe case below stronger than
 * the same-transaction one in {@code NotificationCreateOnceIntegrationTest}: the second insert meets a
 * row that another transaction already committed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class NotificationApiOwnershipIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String ENTITY_TYPE = "PURCHASE_REQUEST";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    /** Users A and B share an organization; the outsider sits in a second one. */
    private String emailOfA;
    private String emailOfB;
    private String emailOfOutsider;
    private UUID notificationOfA;
    private UUID notificationOfB;

    @BeforeEach
    void createTwoRecipientsAndAnOutsider() {
        Organization organization = newOrganization("api-own");
        emailOfA = newUser(organization);
        emailOfB = newUser(organization);
        emailOfOutsider = newUser(newOrganization("api-other"));

        notificationOfA = notifyAboutANewEntity(userIdOf(emailOfA), "Addressed to A", "A must see this");
        notificationOfB = notifyAboutANewEntity(userIdOf(emailOfB), "Addressed to B", "B must see this");
    }

    /**
     * Requirement 28.6: the body wording is pinned, so it is asserted and not just the status. The
     * notification is left untouched - a 404 that still flipped the flag would be worse than a 200.
     */
    @Test
    void markingAnotherUsersNotificationReadIsNotFoundWithThePinnedMessage() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/{id}/read", notificationOfB).with(as(emailOfA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Notification not found"));

        assertThat(notificationRepository.findById(notificationOfB).orElseThrow().isRead()).isFalse();
    }

    /**
     * Tenant isolation surfaces as not-found, not as forbidden: the recipient is part of the lookup,
     * so a caller from another organization never learns the notification exists.
     */
    @Test
    void markingANotificationFromAnotherOrganizationIsAlsoNotFoundRatherThanForbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/{id}/read", notificationOfB)
                        .with(as(emailOfOutsider)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notification not found"));

        assertThat(notificationRepository.findById(notificationOfB).orElseThrow().isRead()).isFalse();
    }

    /** Requirement 28.5: the same endpoint that refuses a foreign row accepts the caller's own. */
    @Test
    void markingOwnNotificationReadSucceedsAndTheFollowUpListShowsIt() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/{id}/read", notificationOfA).with(as(emailOfA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/notifications").with(as(emailOfA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(notificationOfA.toString()))
                .andExpect(jsonPath("$.data.content[0].read").value(true));
    }

    /** Requirement 28.3: the list is addressed to the caller, with no parameter able to widen it. */
    @Test
    void listReturnsOnlyTheCallersOwnNotifications() throws Exception {
        mockMvc.perform(get("/api/v1/notifications").with(as(emailOfA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(notificationOfA.toString()))
                .andExpect(jsonPath("$.data.content[*].id")
                        .value(Matchers.not(Matchers.hasItem(notificationOfB.toString()))));
    }

    /**
     * Requirement 28.9 across commit boundaries. The first call commits before the second starts - the
     * read in between proves it, because it runs in a transaction of its own - so the second insert
     * meets an already-committed row rather than one its own transaction wrote. Exactly one row
     * survives, and every column the second call supplied a value for is still the first call's.
     */
    @Test
    void createOnceDeduplicatesAcrossSeparatelyCommittedTransactions() {
        UUID recipientId = userIdOf(emailOfB);
        UUID entityId = UUID.randomUUID();

        notificationService.createOnce(recipientId, NotificationEvent.INVOICE_SUBMITTED,
                ENTITY_TYPE, entityId, "First title", "First message");
        assertThat(committedNotificationsOf(recipientId)).hasSize(2);
        Notification asFirstCommitted = onlyNotificationFor(recipientId, entityId);

        notificationService.createOnce(recipientId, NotificationEvent.INVOICE_SUBMITTED,
                ENTITY_TYPE, entityId, "Second title", "Second message");

        Notification deduped = onlyNotificationFor(recipientId, entityId);
        assertThat(deduped.getId()).isEqualTo(asFirstCommitted.getId());
        assertThat(deduped.getTitle()).isEqualTo("First title");
        assertThat(deduped.getMessage()).isEqualTo("First message");
        assertThat(deduped.isRead()).isFalse();
        assertThat(deduped.getCreatedAt()).isEqualTo(asFirstCommitted.getCreatedAt());
    }

    /**
     * Requirement 28.7 read as a boundary rather than a bulk update: the one statement behind
     * mark-all-read is keyed on the caller, so a second recipient's unread notifications survive it.
     * Requirement 28.8 is asserted on the same rows, since an unread count that leaked across users
     * would be the same defect seen from the read side.
     */
    @Test
    void markAllReadAndUnreadCountNeverReachAnotherUsersNotifications() throws Exception {
        UUID idOfA = userIdOf(emailOfA);
        UUID idOfB = userIdOf(emailOfB);
        notifyAboutANewEntity(idOfA, "Second for A", "A must see this too");
        notifyAboutANewEntity(idOfB, "Second for B", "B must see this too");

        assertUnreadCount(emailOfA, 2);
        assertUnreadCount(emailOfB, 2);

        mockMvc.perform(patch("/api/v1/notifications/read-all").with(as(emailOfA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertUnreadCount(emailOfA, 0);
        assertUnreadCount(emailOfB, 2);
        assertThat(committedNotificationsOf(idOfA)).allMatch(Notification::isRead);
        assertThat(committedNotificationsOf(idOfB)).noneMatch(Notification::isRead);
    }

    private void assertUnreadCount(String email, long expected) throws Exception {
        mockMvc.perform(get("/api/v1/notifications/unread-count").with(as(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value((int) expected));
    }

    private Notification onlyNotificationFor(UUID recipientId, UUID entityId) {
        return committedNotificationsOf(recipientId).stream()
                .filter(notification -> entityId.equals(notification.getEntityId()))
                .reduce((first, second) -> {
                    throw new AssertionError("Expected exactly one notification for the dedupe key");
                })
                .orElseThrow();
    }

    private RequestPostProcessor as(String email) {
        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(email);
        return authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Organization newOrganization(String slugPrefix) {
        Organization organization = new Organization();
        organization.setName("Notification API Test Org");
        organization.setSlug(slugPrefix + "-" + UUID.randomUUID());
        return organizationRepository.saveAndFlush(organization);
    }

    private String newUser(Organization organization) {
        User user = new User();
        user.setOrganization(organization);
        user.setEmail("recipient-" + UUID.randomUUID() + "@demo-corp.com");
        user.setPasswordHash("hash");
        user.setFirstName("Test");
        user.setLastName("Recipient");
        return userRepository.saveAndFlush(user).getEmail();
    }

    private UUID userIdOf(String email) {
        return ((UserPrincipal) userDetailsService.loadUserByUsername(email)).getId();
    }

    private UUID notifyAboutANewEntity(UUID recipientId, String title, String message) {
        UUID entityId = UUID.randomUUID();
        notificationService.createOnce(recipientId, NotificationEvent.PURCHASE_REQUEST_SUBMITTED,
                ENTITY_TYPE, entityId, title, message);
        return committedNotificationsOf(recipientId).stream()
                .filter(notification -> entityId.equals(notification.getEntityId()))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private List<Notification> committedNotificationsOf(UUID recipientId) {
        return notificationRepository.findByUserId(recipientId, PageRequest.of(0, 20)).getContent();
    }
}
