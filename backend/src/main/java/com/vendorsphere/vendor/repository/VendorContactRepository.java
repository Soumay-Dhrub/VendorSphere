package com.vendorsphere.vendor.repository;

import com.vendorsphere.vendor.entity.VendorContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contacts hold no organization column of their own, so every finder traverses
 * {@code vendor.organization.id}. A cross-tenant identifier therefore misses and surfaces as 404
 * rather than 403 (Requirement 30.10).
 */
public interface VendorContactRepository extends JpaRepository<VendorContact, UUID> {

    Optional<VendorContact> findByIdAndVendorOrganizationId(UUID id, UUID organizationId);

    /** Primary contact first, remaining contacts by name ascending (Requirement 4.3). */
    List<VendorContact> findByVendorIdAndVendorOrganizationIdOrderByPrimaryContactDescNameAsc(
            UUID vendorId, UUID organizationId);
}
