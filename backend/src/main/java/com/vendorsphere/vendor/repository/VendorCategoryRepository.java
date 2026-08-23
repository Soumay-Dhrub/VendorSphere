package com.vendorsphere.vendor.repository;

import com.vendorsphere.vendor.entity.VendorCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Every finder is keyed on the organization, so a cross-tenant identifier misses and surfaces as
 * 404 rather than 403 (Requirement 30.10).
 */
public interface VendorCategoryRepository extends JpaRepository<VendorCategory, UUID> {

    Optional<VendorCategory> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<VendorCategory> findByOrganizationIdOrderByNameAsc(UUID organizationId);

    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
}
