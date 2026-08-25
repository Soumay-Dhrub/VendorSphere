package com.vendorsphere.rfq.repository;

import com.vendorsphere.rfq.RfqStatus;
import com.vendorsphere.rfq.entity.Rfq;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RFQ reads. Every derived finder is keyed on the organization; cross-tenant identifiers miss as 404
 * (Requirement 30.10). {@link JpaSpecificationExecutor} backs the paginated listing of Requirement
 * 31.1 through {@link RfqSpecifications}.
 */
public interface RfqRepository extends JpaRepository<Rfq, UUID>, JpaSpecificationExecutor<Rfq> {

    Optional<Rfq> findByIdAndOrganizationId(UUID id, UUID organizationId);

    /** Overdue OPEN RFQs for the closing job (Requirement 11.3). */
    List<Rfq> findByStatusAndClosingDateBefore(RfqStatus status, Instant instant);

    /**
     * OPEN RFQs whose closing date falls inside the nudge window, for the closing job's reminder run
     * (Requirement 11.5). The caller supplies now + 24h and now + 25h.
     */
    List<Rfq> findByStatusAndClosingDateBetween(RfqStatus status, Instant from, Instant to);

    /**
     * The RFQs of one organization that one vendor holds invitations to, restricted to the statuses a
     * vendor may see and newest first - the vendor-facing listing of Requirement 10.7. The vendor is
     * scoped to the same organization, so a foreign vendor id simply yields nothing.
     */
    @Query("""
            SELECT r FROM Rfq r
            WHERE r.organization.id = :organizationId
              AND EXISTS (SELECT 1 FROM RfqVendor rv
                          WHERE rv.rfq.id = r.id AND rv.vendor.id = :vendorId)
              AND r.status IN :statuses
            ORDER BY r.createdAt DESC
            """)
    List<Rfq> findInvitedForVendor(
            @Param("organizationId") UUID organizationId,
            @Param("vendorId") UUID vendorId,
            @Param("statuses") Collection<RfqStatus> statuses,
            Pageable pageable);

    /**
     * Invited vendors of OPEN RFQs in the nudge window who have not submitted any quotation yet, as
     * {@code [rfq_id, vendor_id]} rows.
     *
     * <p>The quotations table exists from V1 but its module lands with task 9; like the other
     * forward references on this codebase this is a native projection rather than an entity mapping.
     * "Submitted" means a quotation row in any state beyond DRAFT - REJECTED still counts as having
     * responded, because the vendor did quote before being turned down.
     */
    @Query(value = """
            SELECT rv.rfq_id, rv.vendor_id
            FROM rfq_vendors rv
            JOIN rfqs r ON r.id = rv.rfq_id
            WHERE r.status = 'OPEN'
              AND r.closing_date BETWEEN :from AND :to
              AND NOT EXISTS (
                    SELECT 1 FROM quotations q
                    WHERE q.rfq_id = rv.rfq_id
                      AND q.vendor_id = rv.vendor_id
                      AND q.status <> 'DRAFT')
            """, nativeQuery = true)
    List<Object[]> findUnresponsiveInvitations(
            @Param("from") Instant from, @Param("to") Instant to);

    /**
     * Rejects every in-flight quotation of an RFQ on cancellation (Requirement 11.7). A bulk native
     * statement rather than entity loads because the quotation module owns that table; this is the
     * one write the cancellation makes into it.
     *
     * @return how many quotations were rejected
     */
    @Modifying
    @Query(value = """
            UPDATE quotations SET status = 'REJECTED', updated_at = NOW()
            WHERE rfq_id = :rfqId AND status IN ('SUBMITTED', 'UNDER_REVIEW')
            """, nativeQuery = true)
    int rejectInFlightQuotations(@Param("rfqId") UUID rfqId);
}
