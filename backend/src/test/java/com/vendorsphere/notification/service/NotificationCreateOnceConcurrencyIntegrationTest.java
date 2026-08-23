package com.vendorsphere.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.entity.Notification;
import com.vendorsphere.notification.repository.NotificationRepository;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.organization.repository.OrganizationRepository;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Requirement 28.9 under genuine contention: two callers writing the same notification key at the
 * same time on separate connections leave exactly one row behind, and neither caller fails.
 *
 * <p>This is the case the same-transaction test in {@code NotificationCreateOnceIntegrationTest}
 * cannot reach. There the two calls share one transaction, so the second insert conflicts with a row
 * its own transaction wrote. Here the conflicting rows are written by transactions that cannot see
 * each other, which is the situation a read-then-write dedupe would get wrong: both callers would
 * find the notification absent and both would insert.
 *
 * <p>Nothing in this class is {@code @Transactional}. Each thread runs its own
 * {@link TransactionTemplate} call, so it gets its own transaction on its own JDBC connection, and a
 * {@link CyclicBarrier} holds the threads until both are ready so the inserts really do overlap. The
 * recipient is created in a committed transaction beforehand, because the concurrent transactions
 * have to be able to see it.
 *
 * <p>Each thread also writes a second notification for a key of its own after the contended one. That
 * write is the poisoning check: if the conflict had escaped as a constraint violation, PostgreSQL
 * would have aborted the losing transaction and this follow-up write could not commit.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class NotificationCreateOnceConcurrencyIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String ENTITY_TYPE = "PURCHASE_REQUEST";
    private static final NotificationEvent EVENT = NotificationEvent.PURCHASE_REQUEST_SUBMITTED;

    /** Kept well below the default HikariCP maximum pool size so every thread can hold a connection. */
    private static final int THREADS = 4;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private UUID recipientId;

    @BeforeEach
    void createCommittedRecipient() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        recipientId = transactionTemplate.execute(status -> {
            Organization organization = new Organization();
            organization.setName("Concurrent Notification Test Org");
            organization.setSlug("notif-conc-" + UUID.randomUUID());
            organizationRepository.saveAndFlush(organization);

            User user = new User();
            user.setOrganization(organization);
            user.setEmail("recipient-" + UUID.randomUUID() + "@demo-corp.com");
            user.setPasswordHash("hash");
            user.setFirstName("Concurrent");
            user.setLastName("Recipient");
            return userRepository.saveAndFlush(user).getId();
        });
    }

    @Test
    void twoConcurrentCreateOnceCallsForOneKeyLeaveExactlyOneRow() throws Exception {
        UUID contendedEntityId = UUID.randomUUID();

        List<UUID> privateEntityIds = createOnceConcurrently(2, contendedEntityId);

        assertThat(notificationsFor(contendedEntityId)).hasSize(1);
        assertOnlyRowIsUntouched(contendedEntityId);
        assertNoTransactionWasPoisoned(privateEntityIds);
    }

    /**
     * The same guarantee with more contenders, so the outcome does not depend on a single loser
     * queueing behind a single winner.
     */
    @Test
    void manyConcurrentCreateOnceCallsForOneKeyStillLeaveExactlyOneRow() throws Exception {
        UUID contendedEntityId = UUID.randomUUID();

        List<UUID> privateEntityIds = createOnceConcurrently(THREADS, contendedEntityId);

        assertThat(notificationsFor(contendedEntityId)).hasSize(1);
        assertOnlyRowIsUntouched(contendedEntityId);
        assertNoTransactionWasPoisoned(privateEntityIds);
    }

    /**
     * Runs {@code threads} {@code createOnce} calls for {@code contendedEntityId} simultaneously, each
     * in its own transaction, and returns the per-thread entity identifiers whose notifications were
     * written after the contended one. A thread that throws surfaces as a failed {@link Future}, so
     * "no caller fails" is asserted by this method returning at all.
     */
    private List<UUID> createOnceConcurrently(int threads, UUID contendedEntityId) throws Exception {
        CyclicBarrier startTogether = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<UUID>> pending = new ArrayList<>(threads);
            for (int thread = 0; thread < threads; thread++) {
                String title = "Contended title " + thread;
                pending.add(pool.submit(() -> {
                    startTogether.await(30, TimeUnit.SECONDS);
                    return transactionTemplate.execute(status -> {
                        notificationService.createOnce(recipientId, EVENT, ENTITY_TYPE,
                                contendedEntityId, title, "Contended message");
                        UUID privateEntityId = UUID.randomUUID();
                        notificationService.createOnce(recipientId, EVENT, ENTITY_TYPE,
                                privateEntityId, "Uncontended title", "Uncontended message");
                        return privateEntityId;
                    });
                }));
            }
            List<UUID> privateEntityIds = new ArrayList<>(threads);
            for (Future<UUID> call : pending) {
                privateEntityIds.add(call.get(60, TimeUnit.SECONDS));
            }
            return privateEntityIds;
        } finally {
            pool.shutdownNow();
        }
    }

    /** Whichever caller won, the surviving row is a complete unread notification of that caller's. */
    private void assertOnlyRowIsUntouched(UUID contendedEntityId) {
        Notification survivor = notificationsFor(contendedEntityId).getFirst();
        assertThat(survivor.getTitle()).startsWith("Contended title ");
        assertThat(survivor.getMessage()).isEqualTo("Contended message");
        assertThat(survivor.getEventType()).isEqualTo(EVENT);
        assertThat(survivor.isRead()).isFalse();
        assertThat(survivor.getCreatedAt()).isNotNull();
    }

    /** Every thread's follow-up write committed, so no thread's transaction was aborted. */
    private void assertNoTransactionWasPoisoned(List<UUID> privateEntityIds) {
        assertThat(privateEntityIds).doesNotHaveDuplicates();
        for (UUID privateEntityId : privateEntityIds) {
            assertThat(notificationsFor(privateEntityId))
                    .as("follow-up notification for entity %s", privateEntityId)
                    .hasSize(1);
        }
    }

    private List<Notification> notificationsFor(UUID entityId) {
        return notificationRepository.findByUserId(recipientId, PageRequest.of(0, 50)).getContent()
                .stream()
                .filter(notification -> entityId.equals(notification.getEntityId()))
                .toList();
    }
}
