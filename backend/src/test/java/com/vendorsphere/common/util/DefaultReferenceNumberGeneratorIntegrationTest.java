package com.vendorsphere.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.vendorsphere.common.repository.ReferenceSequenceRepository;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.organization.repository.OrganizationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises the real {@code UPDATE ... RETURNING} allocation against PostgreSQL, because the upsert
 * and the returned value are database behaviour that a mock cannot demonstrate.
 *
 * <p>The generator is built by hand with a fixed clock so the year segment is deterministic; the
 * repository under it is the container-backed Spring bean.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class DefaultReferenceNumberGeneratorIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final Clock CLOCK_2026 =
            Clock.fixed(Instant.parse("2026-03-04T10:15:30Z"), ZoneOffset.UTC);

    @Autowired
    private ReferenceSequenceRepository sequenceRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private ReferenceNumberGenerator generator(Clock clock) {
        return new DefaultReferenceNumberGenerator(sequenceRepository, clock);
    }

    private UUID newOrganization() {
        Organization organization = new Organization();
        organization.setName("Reference Test Org");
        organization.setSlug("ref-" + UUID.randomUUID());
        return organizationRepository.saveAndFlush(organization).getId();
    }

    @Test
    @Transactional
    void firstAllocationIsZeroPaddedOneAndSubsequentOnesIncrement() {
        UUID organizationId = newOrganization();
        ReferenceNumberGenerator generator = generator(CLOCK_2026);

        assertThat(generator.allocate(organizationId, ReferencePrefix.RFQ)).isEqualTo("RFQ-2026-001");
        assertThat(generator.allocate(organizationId, ReferencePrefix.RFQ)).isEqualTo("RFQ-2026-002");
        assertThat(generator.allocate(organizationId, ReferencePrefix.RFQ)).isEqualTo("RFQ-2026-003");
    }

    @Test
    @Transactional
    void sequencesAreIndependentPerPrefix() {
        UUID organizationId = newOrganization();
        ReferenceNumberGenerator generator = generator(CLOCK_2026);

        generator.allocate(organizationId, ReferencePrefix.PO);
        generator.allocate(organizationId, ReferencePrefix.PO);

        assertThat(generator.allocate(organizationId, ReferencePrefix.DEL)).isEqualTo("DEL-2026-001");
        assertThat(generator.allocate(organizationId, ReferencePrefix.PO)).isEqualTo("PO-2026-003");
    }

    @Test
    @Transactional
    void sequencesAreIndependentPerOrganization() {
        UUID first = newOrganization();
        UUID second = newOrganization();
        ReferenceNumberGenerator generator = generator(CLOCK_2026);

        generator.allocate(first, ReferencePrefix.VEN);
        generator.allocate(first, ReferencePrefix.VEN);

        assertThat(generator.allocate(second, ReferencePrefix.VEN)).isEqualTo("VEN-2026-001");
    }

    @Test
    @Transactional
    void sequencesRestartInTheFollowingYear() {
        UUID organizationId = newOrganization();

        generator(CLOCK_2026).allocate(organizationId, ReferencePrefix.PR);
        generator(CLOCK_2026).allocate(organizationId, ReferencePrefix.PR);

        Clock clock2027 = Clock.fixed(Instant.parse("2027-01-01T00:00:00Z"), ZoneOffset.UTC);
        assertThat(generator(clock2027).allocate(organizationId, ReferencePrefix.PR))
                .isEqualTo("PR-2027-001");
    }

    @Test
    @Transactional
    void allocationPersistsTheCounterOnTheCallersTransaction() {
        UUID organizationId = newOrganization();

        generator(CLOCK_2026).allocate(organizationId, ReferencePrefix.VEN);
        generator(CLOCK_2026).allocate(organizationId, ReferencePrefix.VEN);

        assertThat(
                        sequenceRepository
                                .findByOrganizationIdAndPrefixAndYear(organizationId, "VEN", 2026)
                                .orElseThrow()
                                .getNextValue())
                .isEqualTo(2);
    }
}
