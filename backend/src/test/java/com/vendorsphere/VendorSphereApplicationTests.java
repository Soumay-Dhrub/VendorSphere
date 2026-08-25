package com.vendorsphere;

import static org.assertj.core.api.Assertions.assertThat;

import com.vendorsphere.testsupport.AbstractIntegrationTest;
import com.vendorsphere.user.RoleName;
import com.vendorsphere.user.entity.Role;
import com.vendorsphere.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class VendorSphereApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        assertThat(userRepository).isNotNull();
    }

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
