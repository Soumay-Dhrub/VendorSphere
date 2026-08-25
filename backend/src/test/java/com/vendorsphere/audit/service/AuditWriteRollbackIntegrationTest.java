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

    @AfterEach
    void removeProbeRows() {
        organizationRepository.findBySlug(slug).ifPresent(organization -> {
            jdbcTemplate.update("DELETE FROM audit_logs WHERE entity_id = ?", organization.getId());
            organizationRepository.deleteById(organization.getId());
        });
        deleteCommittedFixtures();
    }

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

    public enum Failure {
        NONE,

        UNSERIALIZABLE_PAYLOAD,

        UNPERSISTABLE_ROW
    }

    public static class CoupledChangeProbe {

        private final OrganizationRepository organizations;
        private final AuditService auditService;

        CoupledChangeProbe(OrganizationRepository organizations, AuditService auditService) {
            this.organizations = organizations;
            this.auditService = auditService;
        }

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
