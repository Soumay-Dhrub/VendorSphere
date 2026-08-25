package com.vendorsphere.common.util;

import com.vendorsphere.common.repository.ReferenceSequenceRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

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
