package com.vendorsphere.audit.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.entity.AuditLog;
import com.vendorsphere.audit.repository.AuditLogRepository;
import com.vendorsphere.auth.security.UserPrincipal;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.organization.repository.OrganizationRepository;
import com.vendorsphere.user.entity.Role;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.user.repository.UserRepository;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Unit tests for what a recorded audit row carries (Requirement 29.1).
 *
 * <p>Collaborators are JDK proxies rather than Mockito mocks, matching the approach the attachment
 * tests already take so the suite runs without bytecode instrumentation.
 */
class AuditServiceImplTest {

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID ENTITY_ID = UUID.randomUUID();

    private final List<AuditLog> saved = new ArrayList<>();
    private AuditServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuditServiceImpl(
                auditLogRepository(),
                organizationRepository(),
                userRepository(),
                new AuditPayloadSerializer());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void recordsAction_entity_actor_organization_statesAndRequestMetadata() {
        authenticate();
        request(withUserAgent("Mozilla/5.0 (audit test)"));

        service.record(
                AuditAction.VENDOR_STATUS_CHANGED,
                "Vendor",
                ENTITY_ID,
                Map.of("status", "PROSPECTIVE"),
                Map.of("status", "ACTIVE"));

        AuditLog entry = onlySaved();
        assertThat(entry.getAction()).isEqualTo(AuditAction.VENDOR_STATUS_CHANGED);
        assertThat(entry.getEntityType()).isEqualTo("Vendor");
        assertThat(entry.getEntityId()).isEqualTo(ENTITY_ID);
        assertThat(entry.getOrganization().getId()).isEqualTo(ORGANIZATION_ID);
        assertThat(entry.getActor().getId()).isEqualTo(ACTOR_ID);
        assertThat(entry.getPreviousValue()).isEqualTo("{\"status\":\"PROSPECTIVE\"}");
        assertThat(entry.getNewValue()).isEqualTo("{\"status\":\"ACTIVE\"}");
        assertThat(entry.getUserAgent()).isEqualTo("Mozilla/5.0 (audit test)");
        assertThat(entry.getIpAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void prefersTheFirstForwardedForHopOverTheProxyAddress() {
        authenticate();
        MockHttpServletRequest servletRequest = withUserAgent("agent");
        servletRequest.addHeader("X-Forwarded-For", "203.0.113.7, 198.51.100.1, 10.0.0.4");
        request(servletRequest);

        service.record(AuditAction.PAYMENT_RECORDED, "Payment", ENTITY_ID, null, Map.of("a", 1));

        assertThat(onlySaved().getIpAddress()).isEqualTo("203.0.113.7");
    }

    @Test
    void truncatesAnOverlongForwardedAddressRatherThanBreakingTheBusinessChange() {
        authenticate();
        MockHttpServletRequest servletRequest = withUserAgent("agent");
        servletRequest.addHeader("X-Forwarded-For", "x".repeat(120));
        request(servletRequest);

        service.record(AuditAction.DELIVERY_RECORDED, "Delivery", ENTITY_ID, null, Map.of("a", 1));

        assertThat(onlySaved().getIpAddress()).hasSize(45);
    }

    @Test
    void recordsASystemEntryWhenNoRequestOrPrincipalIsAvailable() {
        // A scheduled job changes state outside any request, and audit_logs allows null actor rows.
        service.record(AuditAction.RFQ_STATUS_CHANGED, "Rfq", ENTITY_ID, null, Map.of("status", "CLOSED"));

        AuditLog entry = onlySaved();
        assertThat(entry.getOrganization()).isNull();
        assertThat(entry.getActor()).isNull();
        assertThat(entry.getIpAddress()).isNull();
        assertThat(entry.getUserAgent()).isNull();
        assertThat(entry.getNewValue()).isEqualTo("{\"status\":\"CLOSED\"}");
    }

    @Test
    void neverWritesACredentialIntoTheTrailEvenWhenTheCallerPassesOne() {
        authenticate();
        request(withUserAgent("agent"));

        service.record(
                AuditAction.VENDOR_UPDATED,
                "Vendor",
                ENTITY_ID,
                Map.of("email", "old@example.test", "passwordHash", "$2a$10$leaked"),
                Map.of("email", "new@example.test", "refreshToken", "rt-leaked"));

        AuditLog entry = onlySaved();
        assertThat(entry.getPreviousValue())
                .contains("old@example.test")
                .doesNotContain("$2a$10$leaked");
        assertThat(entry.getNewValue()).contains("new@example.test").doesNotContain("rt-leaked");
    }

    // ----- helpers and doubles -----

    private AuditLog onlySaved() {
        assertThat(saved).hasSize(1);
        return saved.getFirst();
    }

    private static MockHttpServletRequest withUserAgent(String userAgent) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader("User-Agent", userAgent);
        return servletRequest;
    }

    private static void request(MockHttpServletRequest servletRequest) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));
    }

    private AuditLogRepository auditLogRepository() {
        return stub(AuditLogRepository.class, (proxy, method, args) -> {
            if ("save".equals(method.getName())) {
                AuditLog auditLog = (AuditLog) args[0];
                saved.add(auditLog);
                return auditLog;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private static OrganizationRepository organizationRepository() {
        Organization organization = new Organization();
        organization.setId(ORGANIZATION_ID);
        return stub(OrganizationRepository.class, (proxy, method, args) -> {
            if ("getReferenceById".equals(method.getName())) {
                assertThat(args[0]).isEqualTo(ORGANIZATION_ID);
                return organization;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private static UserRepository userRepository() {
        User actor = actor();
        return stub(UserRepository.class, (proxy, method, args) -> {
            if ("getReferenceById".equals(method.getName())) {
                assertThat(args[0]).isEqualTo(ACTOR_ID);
                return actor;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<? super T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                AuditServiceImplTest.class.getClassLoader(), new Class<?>[] {type}, handler);
    }

    private static void authenticate() {
        UserPrincipal principal = new UserPrincipal(actor());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()));
    }

    private static User actor() {
        Organization organization = new Organization();
        organization.setId(ORGANIZATION_ID);

        Role role = new Role();
        role.setName("ADMIN");

        User user = new User();
        user.setId(ACTOR_ID);
        user.setOrganization(organization);
        user.setEmail("admin@example.test");
        user.setPasswordHash("irrelevant");
        user.setRoles(Set.of(role));
        return user;
    }
}
