package com.vendorsphere.common.util;

import com.vendorsphere.common.repository.ReferenceSequenceRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Sequence-table backed {@link ReferenceNumberGenerator}.
 *
 * <p>Carries no {@code @Transactional} annotation on purpose: the allocation must join the
 * transaction of the record that will carry the number, so that the number and the record commit or
 * roll back together (Requirement 1.5). A new transaction here would hand out numbers that survive a
 * failed insert.
 *
 * <p>The calendar year comes from an injected {@link Clock} rather than {@code LocalDate.now()} so
 * that year-boundary behaviour is testable.
 */
@Component
public class DefaultReferenceNumberGenerator implements ReferenceNumberGenerator {

    private final ReferenceSequenceRepository repository;
    private final Clock clock;

    public DefaultReferenceNumberGenerator(ReferenceSequenceRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public String allocate(UUID organizationId, ReferencePrefix prefix) {
        Objects.requireNonNull(organizationId, "organizationId must not be null");
        Objects.requireNonNull(prefix, "prefix must not be null");
        int year = LocalDate.now(clock).getYear();
        int sequence = repository.allocateNextValue(organizationId, prefix.name(), year);
        return ReferenceNumberFormatter.format(prefix, year, sequence);
    }
}
