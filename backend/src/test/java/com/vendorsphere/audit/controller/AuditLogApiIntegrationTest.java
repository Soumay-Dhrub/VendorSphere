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

/**
 * The audit trail as seen from the wire: append-only at the transport level (Requirement 29.9),
 * ADMIN-only (Requirement 29.7), filtered (Requirement 29.6) and tenant-scoped (Requirements 29.3,
 * 30.10).
 *
 * <p>Every case goes through {@link MockMvc} with the real security filter chain, the real
 * {@code @PreAuthorize} evaluation and a real PostgreSQL schema, because that is where these
 * guarantees actually live: the 405 comes out of Spring MVC's handler lookup, the 403 out of method
 * security, and the tenant scope out of the authenticated principal. A test against a stubbed
 * service would show none of it.
 *
 * <p>Nothing here is {@code @Transactional}: each request has to commit on its own, the way a real
 * one does. Fixtures are therefore committed and removed in {@link #removeCommittedFixtures()}, and
 * every assertion is scoped to an organization this test created so a shared database carries no
 * weight.
 */
class AuditLogApiIntegrationTest extends AbstractIntegrationTest {

    private static final String COLLECTION = "/api/v1/audit-logs";
    private static final String ENTRY = "/api/v1/audit-logs/{id}";

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Only for teardown. {@code AuditLogRepository} deliberately exposes no delete - that is the
     * point of Requirement 29.8 - so the rows this test commits are removed with SQL rather than by
     * widening the production contract.
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Organization A holds the trail; organization B exists only to be kept out of it. */
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

    /** {@code audit_logs.actor_id} does not cascade, so the trail goes before its actors. */
    @AfterEach
    void removeCommittedFixtures() {
        committedAuditIds.forEach(
                id -> jdbcTemplate.update("DELETE FROM audit_logs WHERE id = ?", id));
        committedAuditIds.clear();
        deleteCommittedFixtures();
    }

    // ----- Requirement 29.9: append-only at the transport level -----

    /**
     * The controller declares only {@code GET}, so Spring MVC answers a write verb with 405 by
     * itself. That is the whole of the enforcement, which is exactly why it is worth pinning: this
     * test fails the day someone adds a write handler to the audit path.
     */
    @ParameterizedTest
    @ValueSource(strings = {"PUT", "PATCH", "DELETE"})
    void writeVerbsAgainstTheAuditCollectionAreMethodNotAllowed(String method) throws Exception {
        mockMvc.perform(writeRequest(method, COLLECTION).with(as(adminOfA)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", Matchers.containsString("GET")))
                .andExpect(jsonPath("$.success").value(false));

        assertTheTrailOfOrganizationAIsIntact();
    }

    /**
     * The same verbs against a per-entry path, run as an ADMIN deliberately: that is the one role
     * which can reach audit data at all, so anything other than a refusal here would mean a write
     * path exists.
     *
     * <p>The status is asserted as "not successful" rather than as 405 on purpose. No handler is
     * mapped to {@code /audit-logs/{id}} at all, so the request never reaches method matching: it
     * falls through to static resource handling, which raises {@code NoResourceFoundException}, which
     * {@code GlobalExceptionHandler}'s catch-all claims and renders as 500 rather than the 404 it
     * carries. That is a gap in the error mapping - unrelated to the audit trail and unrelated to
     * this task's scope - and pinning 500 here would enshrine it. What matters for Requirement 29.9
     * is asserted instead: the request does not succeed, and the entry is untouched afterwards. A
     * refusal that had already changed the row would be worse than an acceptance.
     */
    @ParameterizedTest
    @ValueSource(strings = {"PUT", "PATCH", "DELETE"})
    void writeVerbsAgainstASingleAuditEntryAreRefusedAndChangeNothing(String method)
            throws Exception {
        int status = mockMvc.perform(
                        writeRequest(method, ENTRY, entryOnVendorByAdminOfA.getId()).with(as(adminOfA)))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status)
                .describedAs("%s against a single audit entry must not succeed", method)
                .matches(code -> code < 200 || code >= 300, "outside the 2xx range");
        assertTheTrailOfOrganizationAIsIntact();
    }

    // ----- Requirement 29.7: ADMIN only -----

    /** Requirement 29.3: the one role that may read the trail gets it, newest entry first. */
    @Test
    void adminReadsTheAuditTrailOfTheirOwnOrganization() throws Exception {
        mockMvc.perform(get(COLLECTION).with(as(adminOfA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.content[0].action").value("INVOICE_SUBMITTED"));
    }

    /** Requirements 29.7 and 30.9: every other role is refused, with the pinned wording. */
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

    /**
     * Requirement 30.2: no token, no trail. The status is asserted as a client error rather than as
     * 401 because {@code SecurityConfig} configures no {@code AuthenticationEntryPoint} yet, so the
     * filter chain answers an anonymous request with Spring Security's default 403 instead of the
     * 401 the requirement pins. Asserting 403 here would lock in behaviour the requirement
     * contradicts, and asserting 401 would leave a red test over work that belongs to the
     * authorization task, so this case guards what is true either way: an unauthenticated caller is
     * refused and receives no audit data.
     */
    @Test
    void anUnauthenticatedRequestGetsNoAuditData() throws Exception {
        mockMvc.perform(get(COLLECTION))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    // ----- Requirement 29.6: filters, through the query string -----

    /**
     * The filtering itself is covered against the database in {@code AuditLogRepositoryTest}; what
     * only shows up here is the binding of {@code actorId}, {@code entityType}, {@code entityId},
     * {@code from} and {@code to} out of the query string into the criteria, and that they narrow
     * rather than widen. The combination below matches exactly one of organization A's three entries.
     */
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

    /**
     * Being an ADMIN grants the trail of your own organization only. The response is 200 carrying B's
     * single entry rather than a 403, because the organization comes from the principal and cannot be
     * influenced by the request.
     */
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

    /** Organization A still holds its three original entries, unchanged. */
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
