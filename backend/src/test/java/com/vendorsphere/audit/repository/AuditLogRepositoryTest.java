package com.vendorsphere.audit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.dto.AuditSearchCriteria;
import com.vendorsphere.audit.entity.AuditLog;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.testsupport.AbstractIntegrationTest;
import com.vendorsphere.user.entity.User;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

class AuditLogRepositoryTest extends AbstractIntegrationTest {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void storesTheStateAsQueryableJsonRatherThanAnEncodedString() {
        Organization organization = organization();
        User actor = user(organization);

        AuditLog saved = auditLogRepository.save(
                entry(organization, actor, AuditAction.VENDOR_STATUS_CHANGED, UUID.randomUUID(),
                        "{\"status\":\"PROSPECTIVE\"}", "{\"status\":\"ACTIVE\"}"));
        entityManager.flush();

        // -> > reaches into a real jsonb document; it returns null if the column merely holds the
        // JSON text as an encoded string literal.
        String newStatus = jdbcTemplate.queryForObject(
                "SELECT new_value ->> 'status' FROM audit_logs WHERE id = ?",
                String.class,
                saved.getId());
        String previousStatus = jdbcTemplate.queryForObject(
                "SELECT previous_value ->> 'status' FROM audit_logs WHERE id = ?",
                String.class,
                saved.getId());

        assertThat(newStatus).isEqualTo("ACTIVE");
        assertThat(previousStatus).isEqualTo("PROSPECTIVE");
    }

    @Test
    @Transactional
    void readsAreScopedToTheCallersOrganization() {
        Organization mine = organization();
        Organization theirs = organization();
        auditLogRepository.save(entry(mine, user(mine), AuditAction.VENDOR_CREATED, UUID.randomUUID()));
        auditLogRepository.save(
                entry(theirs, user(theirs), AuditAction.VENDOR_CREATED, UUID.randomUUID()));
        entityManager.flush();

        Page<AuditLog> page = auditLogRepository.search(
                mine.getId(), AuditSearchCriteria.none(), PageRequest.of(0, 20, NEWEST_FIRST));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getOrganization().getId()).isEqualTo(mine.getId());
    }

    @Test
    @Transactional
    void filtersByActorEntityTypeAndEntityIdTogether() {
        Organization organization = organization();
        User actor = user(organization);
        User other = user(organization);
        UUID vendorId = UUID.randomUUID();

        auditLogRepository.save(entry(organization, actor, AuditAction.VENDOR_UPDATED, vendorId));
        auditLogRepository.save(entry(organization, other, AuditAction.VENDOR_UPDATED, vendorId));
        auditLogRepository.save(
                entry(organization, actor, AuditAction.INVOICE_SUBMITTED, UUID.randomUUID(),
                        null, "{}", "Invoice"));
        entityManager.flush();

        Page<AuditLog> byActor = auditLogRepository.search(
                organization.getId(),
                new AuditSearchCriteria(actor.getId(), null, null, null, null),
                PageRequest.of(0, 20, NEWEST_FIRST));
        assertThat(byActor.getTotalElements()).isEqualTo(2);

        Page<AuditLog> byActorAndEntity = auditLogRepository.search(
                organization.getId(),
                new AuditSearchCriteria(actor.getId(), "Vendor", vendorId, null, null),
                PageRequest.of(0, 20, NEWEST_FIRST));
        assertThat(byActorAndEntity.getTotalElements()).isEqualTo(1);
        assertThat(byActorAndEntity.getContent().getFirst().getAction())
                .isEqualTo(AuditAction.VENDOR_UPDATED);

        Page<AuditLog> byEntityTypeOnly = auditLogRepository.search(
                organization.getId(),
                new AuditSearchCriteria(null, "Invoice", null, null, null),
                PageRequest.of(0, 20, NEWEST_FIRST));
        assertThat(byEntityTypeOnly.getTotalElements()).isEqualTo(1);
        assertThat(byEntityTypeOnly.getContent().getFirst().getAction())
                .isEqualTo(AuditAction.INVOICE_SUBMITTED);
    }

    @Test
    @Transactional
    void filtersByCreationInstantRangeAndReturnsNewestFirst() throws InterruptedException {
        Organization organization = organization();
        User actor = user(organization);

        AuditLog older = auditLogRepository.save(
                entry(organization, actor, AuditAction.RFQ_CREATED, UUID.randomUUID()));
        entityManager.flush();
        Thread.sleep(5);
        AuditLog newer = auditLogRepository.save(
                entry(organization, actor, AuditAction.RFQ_CANCELLED, UUID.randomUUID()));
        entityManager.flush();

        assertThat(older.getCreatedAt()).isBefore(newer.getCreatedAt());

        Page<AuditLog> both = auditLogRepository.search(
                organization.getId(),
                new AuditSearchCriteria(null, null, null, older.getCreatedAt().minusSeconds(1), null),
                PageRequest.of(0, 20, NEWEST_FIRST));
        assertThat(both.getContent()).extracting(AuditLog::getAction)
                .containsExactly(AuditAction.RFQ_CANCELLED, AuditAction.RFQ_CREATED);

        Page<AuditLog> fromTheNewerOnly = auditLogRepository.search(
                organization.getId(),
                new AuditSearchCriteria(null, null, null, newer.getCreatedAt().minusMillis(1), null),
                PageRequest.of(0, 20, NEWEST_FIRST));
        assertThat(fromTheNewerOnly.getContent()).extracting(AuditLog::getAction)
                .containsExactly(AuditAction.RFQ_CANCELLED);

        Page<AuditLog> upToTheOlderOnly = auditLogRepository.search(
                organization.getId(),
                new AuditSearchCriteria(null, null, null, null, older.getCreatedAt().plusMillis(1)),
                PageRequest.of(0, 20, NEWEST_FIRST));
        assertThat(upToTheOlderOnly.getContent()).extracting(AuditLog::getAction)
                .containsExactly(AuditAction.RFQ_CREATED);
    }

    @Test
    @Transactional
    void pagesTheResult() {
        Organization organization = organization();
        User actor = user(organization);
        for (int i = 0; i < 3; i++) {
            auditLogRepository.save(
                    entry(organization, actor, AuditAction.PAYMENT_RECORDED, UUID.randomUUID(),
                            null, "{}", "Payment"));
        }
        entityManager.flush();

        Page<AuditLog> firstPage = auditLogRepository.search(
                organization.getId(), AuditSearchCriteria.none(),
                PageRequest.of(0, 2, NEWEST_FIRST));

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.isFirst()).isTrue();
        assertThat(firstPage.isLast()).isFalse();
    }

    // ----- fixtures -----

    private Organization organization() {
        return newOrganization("audit");
    }

    private User user(Organization organization) {
        return userRepository.findById(newActor(organization).id()).orElseThrow();
    }

    private AuditLog entry(
            Organization organization, User actor, AuditAction action, UUID entityId) {
        return entry(organization, actor, action, entityId, null, "{}", "Vendor");
    }

    private AuditLog entry(
            Organization organization,
            User actor,
            AuditAction action,
            UUID entityId,
            String previousValue,
            String newValue) {
        return entry(organization, actor, action, entityId, previousValue, newValue, "Vendor");
    }

    private AuditLog entry(
            Organization organization,
            User actor,
            AuditAction action,
            UUID entityId,
            String previousValue,
            String newValue,
            String entityType) {
        AuditLog auditLog = new AuditLog();
        auditLog.setOrganization(organization);
        auditLog.setActor(actor);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setPreviousValue(previousValue);
        auditLog.setNewValue(newValue);
        auditLog.setIpAddress("127.0.0.1");
        auditLog.setUserAgent("integration-test");
        return auditLog;
    }
}
