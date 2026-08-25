package com.vendorsphere.common.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.UUID;

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
