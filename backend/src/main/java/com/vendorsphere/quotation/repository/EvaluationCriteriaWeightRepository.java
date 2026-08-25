package com.vendorsphere.quotation.repository;

import com.vendorsphere.quotation.entity.EvaluationCriteriaWeight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Criteria weights; one row per organization by the V2 UNIQUE constraint. */
public interface EvaluationCriteriaWeightRepository
        extends JpaRepository<EvaluationCriteriaWeight, UUID> {

    Optional<EvaluationCriteriaWeight> findByOrganizationId(UUID organizationId);
}
