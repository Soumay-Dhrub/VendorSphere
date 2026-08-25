package com.vendorsphere.vendor.repository;

import com.vendorsphere.vendor.entity.VendorContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorContactRepository extends JpaRepository<VendorContact, UUID> {

    Optional<VendorContact> findByIdAndVendorOrganizationId(UUID id, UUID organizationId);

    List<VendorContact> findByVendorIdAndVendorOrganizationIdOrderByPrimaryContactDescNameAsc(
            UUID vendorId, UUID organizationId);
}
