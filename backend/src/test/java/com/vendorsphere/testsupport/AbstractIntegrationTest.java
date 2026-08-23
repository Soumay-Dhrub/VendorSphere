package com.vendorsphere.testsupport;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.vendorsphere.auth.security.CustomUserDetailsService;
import com.vendorsphere.auth.security.UserPrincipal;
import com.vendorsphere.auth.service.JwtService;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.organization.repository.OrganizationRepository;
import com.vendorsphere.user.entity.Role;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.user.repository.RoleRepository;
import com.vendorsphere.user.repository.UserRepository;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for every integration test that needs a real PostgreSQL 16 database (task 22.1).
 *
 * <h2>One container, one application context</h2>
 *
 * <p>The container below is {@code static} and started once from a static initializer, so it is
 * shared by every subclass for the whole JVM. It is deliberately <em>not</em> annotated
 * {@code @Container} and this class is deliberately <em>not</em> annotated {@code @Testcontainers}:
 * that pair hands the lifecycle to the JUnit extension, which stops the container after each test
 * class. Ryuk removes it when the JVM exits.
 *
 * <p>The Spring annotations live here and nowhere else. Spring's context cache is keyed on the
 * merged context configuration, so a single extra context-affecting annotation on a subclass
 * ({@code @SpringBootTest} attributes, {@code @ActiveProfiles}, {@code @TestPropertySource},
 * {@code @MockitoBean}, an additional {@code @ServiceConnection} field, ...) forks the cache and
 * builds a second context. Subclasses must therefore carry only {@code @Test},
 * {@code @Transactional}, lifecycle callbacks and {@code @Autowired} fields.
 *
 * <h2>Test isolation on a shared database</h2>
 *
 * <p>Because the database is now shared, isolation is explicit, in this order of preference:
 *
 * <ol>
 *   <li><b>Rollback per test.</b> The default: annotate the test method (or the subclass)
 *       {@code @Transactional} and Spring rolls the test-managed transaction back, so nothing the
 *       test wrote is visible to any later class. Use this unless the test needs committed data.
 *   <li><b>Committed fixtures with explicit teardown.</b> Tests that genuinely need commits — real
 *       HTTP requests in their own transactions, or concurrent transactions contending on a row —
 *       create their fixtures through {@link #newOrganization} / {@link #newActor} and call
 *       {@link #deleteCommittedFixtures()} from an {@code @AfterEach}. Fixtures are removed newest
 *       first, and {@code users} is deleted before {@code organizations} because that foreign key
 *       does not cascade.
 *   <li><b>Assertions scoped to what the test created.</b> Assertions must not assume an otherwise
 *       empty database. Count assertions are expressed as a delta around the rows the test itself
 *       inserts, and lookups are keyed on the test's own generated identifiers and slugs.
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    /** Shared across every subclass; see the class javadoc for why the lifecycle is manual. */
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected OrganizationRepository organizationRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected CustomUserDetailsService userDetailsService;

    @Autowired
    protected JwtService jwtService;

    /** Fixtures created through the helpers below, newest first, for {@link #deleteCommittedFixtures}. */
    private final Deque<UUID> createdUserIds = new ArrayDeque<>();
    private final Deque<UUID> createdOrganizationIds = new ArrayDeque<>();

    /** An authenticated user, with everything a request helper or an assertion needs. */
    public record TestActor(UUID id, UUID organizationId, String email) {}

    // ----- fixtures -----

    /** A fresh organization with a collision-free slug. */
    protected Organization newOrganization(String slugPrefix) {
        Organization organization = new Organization();
        organization.setName("Integration Test Org");
        organization.setSlug(slugPrefix + "-" + UUID.randomUUID());
        Organization saved = organizationRepository.saveAndFlush(organization);
        createdOrganizationIds.push(saved.getId());
        return saved;
    }

    /** A user in a brand new organization, holding the given roles (see {@code RoleName}). */
    protected TestActor newActor(String... roleNames) {
        return newActor(newOrganization("org"), roleNames);
    }

    /** A user in an existing organization, holding the given roles (see {@code RoleName}). */
    protected TestActor newActor(Organization organization, String... roleNames) {
        User user = new User();
        user.setOrganization(organization);
        user.setEmail("actor-" + UUID.randomUUID() + "@integration.test");
        user.setPasswordHash("irrelevant-for-tests");
        user.setFirstName("Integration");
        user.setLastName("Actor");
        user.setRoles(rolesNamed(roleNames));
        User saved = userRepository.saveAndFlush(user);
        createdUserIds.push(saved.getId());
        return new TestActor(saved.getId(), organization.getId(), saved.getEmail());
    }

    /** The seeded {@code roles} rows for the given names; {@code V1} inserts all six. */
    private Set<Role> rolesNamed(String... roleNames) {
        return Arrays.stream(roleNames)
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new IllegalArgumentException("Unseeded role: " + name)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // ----- authenticated request helpers -----

    /**
     * Runs the request as the given actor by placing the real {@link UserPrincipal} — roles and all —
     * in the security context, so {@code @PreAuthorize} and every {@code SecurityUtils} lookup see
     * exactly what a signed-in caller of that role would produce.
     */
    protected RequestPostProcessor as(TestActor actor) {
        return as(actor.email());
    }

    /** As {@link #as(TestActor)}, for a user whose email a test already holds. */
    protected RequestPostProcessor as(String email) {
        UserPrincipal principal = principalOf(email);
        return authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    /**
     * Runs the request with a real signed access token instead of a pre-populated security context,
     * so the JWT filter chain itself is exercised. The actor has to be committed for the filter's
     * user lookup to find it.
     */
    protected RequestPostProcessor withJwt(TestActor actor) {
        String token = jwtService.generateAccessToken(principalOf(actor.email()));
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return request;
        };
    }

    protected UserPrincipal principalOf(String email) {
        return (UserPrincipal) userDetailsService.loadUserByUsername(email);
    }

    // ----- teardown for tests that commit -----

    /**
     * Removes the fixtures this test committed, newest first. Notifications, refresh tokens and
     * {@code user_roles} rows go with their user by {@code ON DELETE CASCADE}; the
     * {@code users -> organizations} foreign key does not cascade, hence the ordering.
     *
     * <p>Transactional tests never need this - their rollback covers it.
     */
    protected void deleteCommittedFixtures() {
        while (!createdUserIds.isEmpty()) {
            userRepository.deleteById(createdUserIds.pop());
        }
        while (!createdOrganizationIds.isEmpty()) {
            organizationRepository.deleteById(createdOrganizationIds.pop());
        }
    }
}
