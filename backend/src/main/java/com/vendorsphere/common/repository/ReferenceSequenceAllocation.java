package com.vendorsphere.common.repository;

import java.util.UUID;

/**
 * Counter allocation fragment of {@link ReferenceSequenceRepository}.
 *
 * <p>Kept as a fragment rather than a derived query method because the increment has to return the
 * value it wrote, which Spring Data's {@code @Modifying} contract cannot express.
 */
public interface ReferenceSequenceAllocation {

    /**
     * Increments and returns the counter for one organization, prefix and year, on the caller's
     * transaction and connection.
     *
     * @return the newly allocated sequence value, {@code 1} for the first allocation of the key
     */
    int allocateNextValue(UUID organizationId, String prefix, int year);
}
