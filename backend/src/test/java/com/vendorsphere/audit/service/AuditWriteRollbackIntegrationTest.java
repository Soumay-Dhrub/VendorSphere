package com.vendorsphere.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.common.dto.ApiResponse;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.organization.repository.OrganizationRepository;
import com.vendorsphere.testsupport.AbstractIntegrationTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Requirement 29.10: when the audit write fails, the business change it accompanies must not
 * survive, and the caller must see 500.
 *
 * <h2>Why this test carries its own endpoint</h2>
 *
 * <p>The guarantee is a property of the transaction that wraps a business change together with its
 * audit write. At this point in the plan the audit module exists but none of the services that call
 * it do - vendors, purchase requests, RFQs and the rest arrive in later tasks - so there is no
 * production endpoint yet that writes an audit row inside a business transaction to aim a test at.
 *
 * <p>Rather than settle for something that only resembles coverage (asserting that the serializer
 * throws, say, which says nothing about rollback), this class registers a probe in test sources only:
 * {@link CoupledChangeProbe} makes a real business change - it inserts an organization - and then
 * calls the real {@link AuditService} from the same {@code @Transactional} method, which is exactly
 * the shape the design prescribes for every state-changing service. No production code was added or
 * changed for it. Everything around the probe is real: the security filter chain, servlet dispatch,
 * the transaction manager, {@code GlobalExceptionHandler} and PostgreSQL.
 *
 * <p>The probe beans arrive through a nested {@link TestConfiguration}, which forks the Spring
 * context for this class alone. That is the one deviation from {@link AbstractIntegrationTest}'s rule
 * about context-affecting annotations, and it is confined to this class; the database container is
 * still the shared one.
 *
 * <p>The control case is what makes the two failure cases mean anything: unless the probe
 * demonstrably commits both rows when the audit write succeeds, a missing organization afterwards
 * would prove nothing.
 */
class AuditWriteRollbackIntegrationTest extends AbstractIntegrationTest {

    private static final String PROBE_PATH = "/api/v1/test-support/coupled-change";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private TestActor actor;
    private String slug;

    @BeforeEach
    void createAnAuthenticatedActor() {
        actor = newActor("ADMIN");
        slug = "coupled-change-" + UUID.randomUUID();
    }

    /** The probe's own rows are not fixtures of the base class, so they are removed by hand. */
    @AfterEach
    void removeProbeRows() {
        organizationRepository.findBySlug(slug).ifPresent(organization -> {
            jdbcTemplate.update("DELETE FROM audit_logs WHERE entity_id = ?", organization.getId());
            organizationRepository.deleteById(organization.getId());
        });
        deleteCommittedFixtures();
    }

    /** The control case: both rows commit, so the failure cases below are showing real rollback. */
    @Test
    void aSucceedingAuditWriteCommitsTogetherWithTheBusinessChange() throws Exception {
        mockMvc.perform(post(PROBE_PATH)
                        .with(as(actor))
                        .param("slug", slug)
                        .param("failure", Failure.NONE.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        UUID organizationId = organizationRepository.findBySlug(slug).orElseThrow().getId();
        assertThat(auditRowsFor(organizationId)).isEqualTo(1);
    }

    /**
     * The audit write fails while serializing the recorded state, which is the failure
     * {@link AuditPayloadSerializer} raises as {@code IllegalStateException}. It happens before a row
     * is built, so the trail keeps no trace of the attempt - and the business insert has to be gone
     * all the same.
     */
    @Test
    void aPayloadThatCannotBeSerializedRollsTheBusinessChangeBackAndAnswers500() throws Exception {
        mockMvc.perform(post(PROBE_PATH)
                        .with(as(actor))
                        .param("slug", slug)
                        .param("failure", Failure.UNSERIALIZABLE_PAYLOAD.name()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));

        assertThat(organizationRepository.findBySlug(slug)).isEmpty();
    }

    /**
     * The audit write fails in the database instead: the row is built and handed to the repository,
     * and PostgreSQL refuses it because {@code audit_logs.entity_type} is {@code NOT NULL}. This is
     * the case acceptance criterion 29.10 describes literally - the Audit_Service fails to persist -
     * and the harsher one, because the failure surfaces only when the shared transaction flushes,
     * after the business insert has already been sent. Both go.
     */
    @Test
    void anAuditRowRejectedByTheDatabaseRollsTheBusinessChangeBackAndAnswers500() throws Exception {
        mockMvc.perform(post(PROBE_PATH)
                        .with(as(actor))
                        .param("slug", slug)
                        .param("failure", Failure.UNPERSISTABLE_ROW.name()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));

        assertThat(organizationRepository.findBySlug(slug)).isEmpty();
    }

    private int auditRowsFor(UUID entityId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_logs WHERE entity_id = ?", Integer.class, entityId);
        return count == null ? 0 : count;
    }

    // ----- test-only probe -----

    /** How the audit write should fail, or {@link #NONE} for the control case. */
    public enum Failure {
        NONE,
        /** Serialization raises before an audit row exists. */
        UNSERIALIZABLE_PAYLOAD,
        /** The row is built but violates {@code audit_logs.entity_type NOT NULL}. */
        UNPERSISTABLE_ROW
    }

    /**
     * Stands in for the state-changing services of later tasks: one transaction, a business change,
     * then the audit write. It deliberately catches nothing - swallowing an audit failure here is
     * precisely what Requirement 29.10 forbids.
     */
    public static class CoupledChangeProbe {

        private final OrganizationRepository organizations;
        private final AuditService auditService;

        CoupledChangeProbe(OrganizationRepository organizations, AuditService auditService) {
            this.organizations = organizations;
            this.auditService = auditService;
        }

        /** Public because Spring's transaction proxy only advises public methods. */
        @Transactional
        public void changeThenAudit(String slug, Failure failure) {
            Organization organization = new Organization();
            organization.setName("Coupled change");
            organization.setSlug(slug);
            UUID entityId = organizations.save(organization).getId();

            auditService.record(
                    AuditAction.VENDOR_CREATED,
                    failure == Failure.UNPERSISTABLE_ROW ? null : "Organization",
                    entityId,
                    null,
                    failure == Failure.UNSERIALIZABLE_PAYLOAD
                            ? new UnserializableState()
                            : Map.of("slug", slug));
        }
    }

    /** Jackson cannot turn this into a tree: reading the property throws. */
    public static final class UnserializableState {
        public String getValue() {
            throw new IllegalStateException("This state cannot be serialized");
        }
    }

    @RestController
    @RequestMapping(PROBE_PATH)
    public static class CoupledChangeProbeController {

        private final CoupledChangeProbe probe;

        CoupledChangeProbeController(CoupledChangeProbe probe) {
            this.probe = probe;
        }

        @PostMapping
        public ApiResponse<Void> changeThenAudit(
                @RequestParam String slug, @RequestParam Failure failure) {
            probe.changeThenAudit(slug, failure);
            return ApiResponse.ok("Committed", null);
        }
    }

    @TestConfiguration
    static class ProbeConfiguration {

        @Bean
        CoupledChangeProbe coupledChangeProbe(
                OrganizationRepository organizations, AuditService auditService) {
            return new CoupledChangeProbe(organizations, auditService);
        }

        @Bean
        CoupledChangeProbeController coupledChangeProbeController(CoupledChangeProbe probe) {
            return new CoupledChangeProbeController(probe);
        }
    }
}
