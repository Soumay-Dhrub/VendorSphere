package com.vendorsphere.common.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.UUID;

/**
 * Allocates reference number sequence values with a single upsert statement.
 *
 * <p>The statement inserts the counter row when the key is new and otherwise increments the existing
 * row, returning the value it wrote:
 *
 * <pre>{@code
 * INSERT INTO reference_sequences (organization_id, prefix, year, next_value)
 * VALUES (?, ?, ?, 1)
 * ON CONFLICT (organization_id, prefix, year)
 * DO UPDATE SET next_value = reference_sequences.next_value + 1
 * RETURNING next_value
 * }</pre>
 *
 * <p>Two properties matter here. First, one statement means the increment holds the row lock for its
 * whole duration, so concurrent transactions queue and receive distinct values instead of both
 * reading the same current value (Requirement 1.6). The {@code ON CONFLICT} branch also covers two
 * concurrent <em>first</em> allocations of the same key: the loser of the unique index race falls
 * into the update branch and receives {@code 2} rather than failing or duplicating {@code 1}.
 * Second, the statement runs on the {@link EntityManager}'s connection, so it commits and rolls back
 * with the caller's transaction (Requirement 1.5). Nothing here opens a new transaction.
 */
class ReferenceSequenceAllocationImpl implements ReferenceSequenceAllocation {

    private static final String ALLOCATE_SQL =
            """
            INSERT INTO reference_sequences (organization_id, prefix, year, next_value)
            VALUES (:organizationId, :prefix, :year, 1)
            ON CONFLICT (organization_id, prefix, year)
            DO UPDATE SET next_value = reference_sequences.next_value + 1
            RETURNING next_value
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public int allocateNextValue(UUID organizationId, String prefix, int year) {
        Object allocated =
                entityManager
                        .createNativeQuery(ALLOCATE_SQL)
                        .setParameter("organizationId", organizationId)
                        .setParameter("prefix", prefix)
                        .setParameter("year", year)
                        .getSingleResult();
        return ((Number) allocated).intValue();
    }
}
