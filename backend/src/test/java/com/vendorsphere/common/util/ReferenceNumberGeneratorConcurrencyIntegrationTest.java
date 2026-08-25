package com.vendorsphere.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.vendorsphere.common.repository.ReferenceSequenceRepository;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.organization.repository.OrganizationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ReferenceNumberGeneratorConcurrencyIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final int YEAR = 2026;
    private static final Clock CLOCK_2026 =
            Clock.fixed(Instant.parse("2026-03-04T10:15:30Z"), ZoneOffset.UTC);
    private static final ReferencePrefix PREFIX = ReferencePrefix.PO;

    private static final int HIGH_CONCURRENCY = 8;

    @Autowired
    private ReferenceSequenceRepository sequenceRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private UUID organizationId;

    @BeforeEach
    void createOrganization() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        organizationId =
                transactionTemplate.execute(
                        status -> {
                            Organization organization = new Organization();
                            organization.setName("Concurrent Reference Test Org");
                            organization.setSlug("ref-conc-" + UUID.randomUUID());
                            return organizationRepository.saveAndFlush(organization).getId();
                        });
    }

    @AfterEach
    void removeTestRows() {
        transactionTemplate.executeWithoutResult(
                status -> {
                    sequenceRepository
                            .findByOrganizationIdAndPrefixAndYear(organizationId, PREFIX.name(), YEAR)
                            .ifPresent(sequenceRepository::delete);
                    organizationRepository.deleteById(organizationId);
                });
    }

    @Test
    void twoConcurrentAllocationsReceiveDistinctSequenceValues() throws Exception {
        List<String> references = allocateConcurrently(2);

        assertThat(references).hasSize(2).doesNotHaveDuplicates();
        assertThat(sequenceValues(references)).containsExactly(1, 2);
        assertThat(counterValue()).isEqualTo(2);
    }

    @Test
    void manyConcurrentAllocationsReceiveDistinctContiguousSequenceValues() throws Exception {
        List<String> references = allocateConcurrently(HIGH_CONCURRENCY);

        assertThat(references).hasSize(HIGH_CONCURRENCY).doesNotHaveDuplicates();
        assertThat(sequenceValues(references))
                .containsExactlyElementsOf(IntStream.rangeClosed(1, HIGH_CONCURRENCY).boxed().toList());
        assertThat(counterValue()).isEqualTo(HIGH_CONCURRENCY);
    }

    private List<String> allocateConcurrently(int threads) throws Exception {
        ReferenceNumberGenerator generator =
                new DefaultReferenceNumberGenerator(sequenceRepository, CLOCK_2026);
        CyclicBarrier startTogether = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<String>> pending = new ArrayList<>(threads);
            for (int i = 0; i < threads; i++) {
                pending.add(
                        pool.submit(
                                () -> {
                                    startTogether.await(30, TimeUnit.SECONDS);
                                    return transactionTemplate.execute(
                                            status -> generator.allocate(organizationId, PREFIX));
                                }));
            }
            List<String> references = new ArrayList<>(threads);
            for (Future<String> allocation : pending) {
                references.add(allocation.get(60, TimeUnit.SECONDS));
            }
            return references;
        } finally {
            pool.shutdownNow();
        }
    }

    private List<Integer> sequenceValues(List<String> references) {
        return references.stream()
                .map(reference -> Integer.parseInt(reference.substring(reference.lastIndexOf('-') + 1)))
                .sorted()
                .toList();
    }

    private int counterValue() {
        return transactionTemplate.execute(
                status ->
                        sequenceRepository
                                .findByOrganizationIdAndPrefixAndYear(organizationId, PREFIX.name(), YEAR)
                                .orElseThrow()
                                .getNextValue());
    }
}
