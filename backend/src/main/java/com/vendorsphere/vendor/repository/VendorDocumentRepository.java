package com.vendorsphere.vendor.repository;

import com.vendorsphere.vendor.entity.VendorDocument;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
