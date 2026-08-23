package com.vendorsphere.vendor.repository;

import com.vendorsphere.vendor.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Every finder is keyed on the organization, so a cross-tenant identifier misses and surfaces as
 * 404 rather than 403 (Requirement 30.10).
 */
public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    Optional<Vendor> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndEmailIgnoreCase(UUID organizationId, String email);

    /** Backs the delete guard of Requirement 4.6, which reports how many vendors use a category. */
    long countByOrganizationIdAndCategoryId(UUID organizationId, UUID categoryId);
}
