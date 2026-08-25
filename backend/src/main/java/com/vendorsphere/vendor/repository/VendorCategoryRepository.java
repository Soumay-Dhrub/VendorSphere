package com.vendorsphere.vendor.repository;

import com.vendorsphere.vendor.entity.VendorCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorCategoryRepository extends JpaRepository<VendorCategory, UUID> {

    Optional<VendorCategory> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<VendorCategory> findByOrganizationIdOrderByNameAsc(UUID organizationId);

    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
}
