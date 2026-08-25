package com.vendorsphere.rfq.repository;

import com.vendorsphere.rfq.RfqVendorStatus;
import com.vendorsphere.rfq.entity.RfqVendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Invitation reads for RFQs. Invitations have no organization column of their own, so every
 * tenant-scoped finder traverses {@code rfq.organization.id} or {@code vendor.organization.id};
 * cross-tenant identifiers miss as 404 (Requirement 30.10).
 */
public interface RfqVendorRepository extends JpaRepository<RfqVendor, UUID> {

    Optional<RfqVendor> findByRfqIdAndVendorId(UUID rfqId, UUID vendorId);

    boolean existsByRfqIdAndVendorOrganizationIdAndStatus(
            UUID rfqId, UUID organizationId, RfqVendorStatus status);

    long countByRfqId(UUID rfqId);

    List<RfqVendor> findByRfqIdOrderByInvitedAtAsc(UUID rfqId);

    /**
     * The invitation of one vendor within one organization, the shape the vendor-facing reads need:
     * a vendor user may only see an RFQ its linked vendor is invited to (Requirement 10.7).
     */
    Optional<RfqVendor> findByRfqIdAndVendorIdAndVendorOrganizationId(
            UUID rfqId, UUID vendorId, UUID organizationId);

    List<RfqVendor> findByRfqIdInAndVendorIdOrderByInvitedAtDesc(
            List<UUID> rfqIds, UUID vendorId);
}
