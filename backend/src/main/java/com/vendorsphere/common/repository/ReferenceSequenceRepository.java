package com.vendorsphere.common.repository;

import com.vendorsphere.common.entity.ReferenceSequence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository over {@code reference_sequences}. The counter increment itself lives in
 * {@link ReferenceSequenceAllocation} because it needs the value returned by the statement.
 */
public interface ReferenceSequenceRepository
        extends JpaRepository<ReferenceSequence, UUID>, ReferenceSequenceAllocation {

    Optional<ReferenceSequence> findByOrganizationIdAndPrefixAndYear(
            UUID organizationId, String prefix, int year);
}
