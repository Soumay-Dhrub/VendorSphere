package com.vendorsphere.quotation.repository;

import com.vendorsphere.quotation.entity.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuotationRepository extends JpaRepository<Quotation, UUID> {

    Optional<Quotation> findByIdAndRfqOrganizationId(UUID id, UUID organizationId);

    Optional<Quotation> findByIdAndRfqOrganizationIdAndVendorId(
            UUID id, UUID organizationId, UUID vendorId);

    Optional<Quotation> findByRfqIdAndVendorId(UUID rfqId, UUID vendorId);

    List<Quotation> findByRfqId(UUID rfqId);
}
