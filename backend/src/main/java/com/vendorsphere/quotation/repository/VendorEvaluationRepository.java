package com.vendorsphere.quotation.repository;

import com.vendorsphere.quotation.entity.VendorEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Evaluation reads. UNIQUE (rfq_id, quotation_id) in V1 means a re-evaluation updates the existing
 * row - {@code findByRfqIdAndQuotationId} is the upsert lookup.
 */
public interface VendorEvaluationRepository extends JpaRepository<VendorEvaluation, UUID> {

    Optional<VendorEvaluation> findByRfqIdAndQuotationId(UUID rfqId, UUID quotationId);

    List<VendorEvaluation> findByRfqId(UUID rfqId);

    Optional<VendorEvaluation> findByQuotationId(UUID quotationId);
}
