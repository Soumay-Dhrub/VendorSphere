package com.vendorsphere.rfq.repository;

import com.vendorsphere.rfq.RfqVendorStatus;
import com.vendorsphere.rfq.entity.RfqVendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RfqVendorRepository extends JpaRepository<RfqVendor, UUID> {

    Optional<RfqVendor> findByRfqIdAndVendorId(UUID rfqId, UUID vendorId);

    boolean existsByRfqIdAndVendorOrganizationIdAndStatus(
            UUID rfqId, UUID organizationId, RfqVendorStatus status);

    long countByRfqId(UUID rfqId);

    List<RfqVendor> findByRfqIdOrderByInvitedAtAsc(UUID rfqId);

    Optional<RfqVendor> findByRfqIdAndVendorIdAndVendorOrganizationId(
            UUID rfqId, UUID vendorId, UUID organizationId);

    List<RfqVendor> findByRfqIdInAndVendorIdOrderByInvitedAtDesc(
            List<UUID> rfqIds, UUID vendorId);
}
