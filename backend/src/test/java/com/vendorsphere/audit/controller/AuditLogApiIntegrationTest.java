package com.vendorsphere.audit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.dto.AuditSearchCriteria;
import com.vendorsphere.audit.entity.AuditLog;
import com.vendorsphere.audit.repository.AuditLogRepository;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.testsupport.AbstractIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class AuditLogApiIntegrationTest extends AbstractIntegrationTest {

    private static final String COLLECTION = "/api/v1/audit-logs";
    private static final String ENTRY = "/api/v1/audit-logs/{id}";

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Organization organizationA;
    private Organization organizationB;
    private TestActor adminOfA;
    private TestActor adminOfB;
    private UUID vendorId;
    private AuditLog entryOnVendorByAdminOfA;

    private final List<UUID> committedAuditIds = new ArrayList<>();

    @BeforeEach
    void seedTwoOrganizationsWithOneAuditTrailEach() {
        organizationA = newOrganization("audit-api-a");
        organizationB = newOrganization("audit-api-b");
        adminOfA = newActor(organizationA, "ADMIN");
        adminOfB = newActor(organizationB, "ADMIN");
        TestActor officerOfA = newActor(organizationA, "PROCUREMENT_OFFICER");

        vendorId = UUID.randomUUID();
        entryOnVendorByAdminOfA =
                recordEntry(organizationA, adminOfA, AuditAction.VENDOR_UPDATED, "Vendor", vendorId);
        recordEntry(organizationA, officerOfA, AuditAction.VENDOR_UPDATED, "Vendor", vendorId);
        recordEntry(organizationA, adminOfA, AuditAction.INVOICE_SUBMITTED, "Invoice",
                UUID.randomUUID());

        recordEntry(organizationB, adminOfB, AuditAction.VENDOR_CREATED, "Vendor", UUID.randomUUID());
    }

    @AfterEach
    void removeCommittedFixtures() {
        committedAuditIds.forEach(
                id -> jdbcTemplate.update("DELETE FROM audit_logs WHERE id = ?", id));
        committedAuditIds.clear();
        deleteCommittedFixtures();
    }

    // ----- Requirement 29.9: append-only at the transport level -----

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "PATCH", "DELETE"})
    void writeVerbsAgainstTheAuditCollectionAreMethodNotAllowed(String method) throws Exception {
        mockMvc.perform(writeRequest(method, COLLECTION).with(as(adminOfA)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", Matchers.containsString("GET")))
                .andExpect(jsonPath("$.success").value(false));

        assertTheTrailOfOrganizationAIsIntact();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "PATCH", "DELETE"})
    void writeVerbsAgainstASingleAuditEntryAreRefusedAndChangeNothing(String method)
            throws Exception {
        mockMvc.perform(
                        writeRequest(method, ENTRY, entryOnVendorByAdminOfA.getId()).with(as(adminOfA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertTheTrailOfOrganizationAIsIntact();
    }

    // ----- Requirement 29.7: ADMIN only -----

    @Test
    void adminReadsTheAuditTrailOfTheirOwnOrganization() throws Exception {
        mockMvc.perform(get(COLLECTION).with(as(adminOfA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.content[0].action").value("INVOICE_SUBMITTED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "PROCUREMENT_MANAGER", "PROCUREMENT_OFFICER", "REQUESTER", "FINANCE", "VENDOR"})
    void everyRoleOtherThanAdminIsForbidden(String roleName) throws Exception {
        TestActor actor = newActor(organizationA, roleName);

        mockMvc.perform(get(COLLECTION).with(as(actor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void anUnauthenticatedRequestGetsNoAuditData() throws Exception {
        mockMvc.perform(get(COLLECTION))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    // ----- Requirement 29.6: filters, through the query string -----

    @Test
    void combinedActorEntityAndRangeFiltersReturnOnlyTheMatchingEntry() throws Exception {
        mockMvc.perform(get(COLLECTION)
                        .with(as(adminOfA))
                        .param("actorId", adminOfA.id().toString())
                        .param("entityType", "Vendor")
                        .param("entityId", vendorId.toString())
                        .param("from",
                                entryOnVendorByAdminOfA.getCreatedAt().minusSeconds(60).toString())
                        .param("to",
                                entryOnVendorByAdminOfA.getCreatedAt().plusSeconds(60).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id")
                        .value(entryOnVendorByAdminOfA.getId().toString()))
                .andExpect(jsonPath("$.data.content[0].action").value("VENDOR_UPDATED"));

        // A range ending before the entry was written excludes it, so the bound survives the trip
        // through the controller instead of being quietly dropped.
        mockMvc.perform(get(COLLECTION)
                        .with(as(adminOfA))
                        .param("actorId", adminOfA.id().toString())
                        .param("to", entryOnVendorByAdminOfA.getCreatedAt().minusMillis(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    // ----- Requirements 29.3 and 30.10: tenant scoping -----

    @Test
    void adminOfAnotherOrganizationSeesNoneOfTheseEntries() throws Exception {
        mockMvc.perform(get(COLLECTION).with(as(adminOfB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].organizationId")
                        .value(organizationB.getId().toString()));

        // Naming A's entity explicitly does not widen the scope either.
        mockMvc.perform(get(COLLECTION)
                        .with(as(adminOfB))
                        .param("entityId", vendorId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    // ----- helpers -----

    private void assertTheTrailOfOrganizationAIsIntact() {
        List<AuditLog> trail = auditLogRepository.search(
                        organizationA.getId(),
                        AuditSearchCriteria.none(),
                        PageRequest.of(0, 20, NEWEST_FIRST))
                .getContent();

        assertThat(trail).hasSize(3);
        AuditLog target = trail.stream()
                .filter(entry -> entry.getId().equals(entryOnVendorByAdminOfA.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The targeted audit entry was removed"));
        assertThat(target.getAction()).isEqualTo(entryOnVendorByAdminOfA.getAction());
        assertThat(target.getEntityType()).isEqualTo(entryOnVendorByAdminOfA.getEntityType());
        assertThat(target.getEntityId()).isEqualTo(entryOnVendorByAdminOfA.getEntityId());
        assertThat(target.getNewValue()).isEqualTo(entryOnVendorByAdminOfA.getNewValue());
    }

    private static MockHttpServletRequestBuilder writeRequest(
            String method, String urlTemplate, Object... variables) {
        return switch (method) {
            case "PUT" -> put(urlTemplate, variables);
            case "PATCH" -> patch(urlTemplate, variables);
            case "DELETE" -> delete(urlTemplate, variables);
            default -> throw new IllegalArgumentException("Unsupported verb " + method);
        };
    }

    private AuditLog recordEntry(
            Organization organization,
            TestActor actor,
            AuditAction action,
            String entityType,
            UUID entityId) {
        AuditLog auditLog = new AuditLog();
        auditLog.setOrganization(organization);
        auditLog.setActor(userRepository.findById(actor.id()).orElseThrow());
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setNewValue("{}");
        auditLog.setIpAddress("127.0.0.1");
        auditLog.setUserAgent("integration-test");
        AuditLog saved = auditLogRepository.save(auditLog);
        committedAuditIds.add(saved.getId());
        return saved;
    }
}
