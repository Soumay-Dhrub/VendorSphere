package com.vendorsphere.vendor.repository;

import com.vendorsphere.vendor.entity.VendorDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Documents hold no organization column of their own, so every finder traverses
 * {@code vendor.organization.id}. A cross-tenant identifier therefore misses and surfaces as 404
 * rather than 403 (Requirement 30.10).
 */
public interface VendorDocumentRepository extends JpaRepository<VendorDocument, UUID> {

    Optional<VendorDocument> findByIdAndVendorOrganizationId(UUID id, UUID organizationId);

    List<VendorDocument> findByVendorIdAndVendorOrganizationIdOrderByUploadedAtDesc(
            UUID vendorId, UUID organizationId);

    /**
     * Counts the vendor's documents whose expiry date falls inside the inclusive window
     * {@code [from, to]}, backing the expiring-document count of Requirement 2.5.
     *
     * <p>Callers pass the request date and the request date plus 30 days, which is the same window
     * the {@code EXPIRING_SOON} state of Requirement 5.4 describes; the document expiry evaluator
     * owns that classification, and this count stays consistent with it. A document without an
     * expiry date is never counted, because {@code BETWEEN} does not match {@code NULL}.
     */
    long countByVendorIdAndVendorOrganizationIdAndExpiryDateBetween(
            UUID vendorId, UUID organizationId, LocalDate from, LocalDate to);
}
