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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

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

    private final Deque<UUID> createdUserIds = new ArrayDeque<>();
    private final Deque<UUID> createdOrganizationIds = new ArrayDeque<>();

    public record TestActor(UUID id, UUID organizationId, String email) {}

    // ----- fixtures -----

    protected Organization newOrganization(String slugPrefix) {
        Organization organization = new Organization();
        organization.setName("Integration Test Org");
        organization.setSlug(slugPrefix + "-" + UUID.randomUUID());
        Organization saved = organizationRepository.saveAndFlush(organization);
        createdOrganizationIds.push(saved.getId());
        return saved;
    }

    protected TestActor newActor(String... roleNames) {
        return newActor(newOrganization("org"), roleNames);
    }

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

    private Set<Role> rolesNamed(String... roleNames) {
        return Arrays.stream(roleNames)
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new IllegalArgumentException("Unseeded role: " + name)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // ----- authenticated request helpers -----

    protected RequestPostProcessor as(TestActor actor) {
        return as(actor.email());
    }

    protected RequestPostProcessor as(String email) {
        UserPrincipal principal = principalOf(email);
        return authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

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

    protected void deleteCommittedFixtures() {
        while (!createdUserIds.isEmpty()) {
            userRepository.deleteById(createdUserIds.pop());
        }
        while (!createdOrganizationIds.isEmpty()) {
            organizationRepository.deleteById(createdOrganizationIds.pop());
        }
    }
}
