package com.vendorsphere.quotation.repository;

import com.vendorsphere.quotation.entity.VendorSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Award records; one per RFQ by the V1 UNIQUE constraint. */
public interface VendorSelectionRepository extends JpaRepository<VendorSelection, UUID> {

    boolean existsByRfqId(UUID rfqId);

    Optional<VendorSelection> findByRfqId(UUID rfqId);
}
