package com.vendorsphere;

import static org.assertj.core.api.Assertions.assertThat;

import com.vendorsphere.testsupport.AbstractIntegrationTest;
import com.vendorsphere.user.RoleName;
import com.vendorsphere.user.entity.Role;
import com.vendorsphere.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Smoke test proving the full application context starts: real DataSource, Flyway
 * migrations, JPA entity manager and Spring Data repositories, all against the shared
 * PostgreSQL 16 container of {@link AbstractIntegrationTest}.
 *
 * <p>Because {@code spring.jpa.hibernate.ddl-auto} is {@code validate}, a successful
 * start also asserts that the JPA entity mappings agree with the schema produced by
 * the Flyway migrations ({@code V1__init_schema.sql} and
 * {@code V2__procurement_lifecycle.sql}).
 */
class VendorSphereApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        assertThat(userRepository).isNotNull();
    }

    /**
     * The migrated schema is not just readable but usable: a user written through JPA lands in the
     * Flyway-created {@code users}, {@code organizations} and {@code user_roles} tables and reads
     * back with its association graph intact.
     *
     * <p>Scoped to the rows this test inserts rather than asserting an empty {@code users} table,
     * because the database is shared with every other integration class (see the isolation notes on
     * {@link AbstractIntegrationTest}). The count is asserted as an exact delta, so a write that
     * silently failed or inserted twice still fails the test.
     */
    @Test
    @Transactional
    void migratedSchemaIsQueryableAndTheUserMappingRoundTrips() {
        long usersBefore = userRepository.count();

        TestActor actor = newActor(RoleName.ADMIN);

        assertThat(userRepository.count()).isEqualTo(usersBefore + 1);

        User persisted = userRepository.findByEmailWithRoles(actor.email()).orElseThrow();
        assertThat(persisted.getId()).isEqualTo(actor.id());
        assertThat(persisted.getOrganization().getId()).isEqualTo(actor.organizationId());
        assertThat(persisted.isActive()).isTrue();
        assertThat(persisted.getRoles()).extracting(Role::getName).containsExactly(RoleName.ADMIN);
    }
}
