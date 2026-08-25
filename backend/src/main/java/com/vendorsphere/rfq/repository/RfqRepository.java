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

public interface RfqRepository extends JpaRepository<Rfq, UUID>, JpaSpecificationExecutor<Rfq> {

    Optional<Rfq> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<Rfq> findByStatusAndClosingDateBefore(RfqStatus status, Instant instant);

    List<Rfq> findByStatusAndClosingDateBetween(RfqStatus status, Instant from, Instant to);

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

    @Modifying
    @Query(value = """
            UPDATE quotations SET status = 'REJECTED', updated_at = NOW()
            WHERE rfq_id = :rfqId AND status IN ('SUBMITTED', 'UNDER_REVIEW')
            """, nativeQuery = true)
    int rejectInFlightQuotations(@Param("rfqId") UUID rfqId);
}
