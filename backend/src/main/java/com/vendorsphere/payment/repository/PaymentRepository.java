package com.vendorsphere.payment.repository;

import com.vendorsphere.invoice.InvoiceStatus;
import com.vendorsphere.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Payment reads and the outstanding-payables aggregate of Requirement 25.10. */
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByInvoiceIdOrderByCreatedAtAscIdAsc(UUID invoiceId);

    /**
     * Outstanding amounts per organization: total minus paid across APPROVED, PARTIALLY_PAID and
     * OVERDUE invoices, as {@code [organization_id, outstanding]} rows.
     */
    @Query(value = """
            SELECT i.organization_id, COALESCE(SUM(i.total_amount - i.paid_amount), 0)
            FROM invoices i
            WHERE i.status IN ('APPROVED', 'PARTIALLY_PAID', 'OVERDUE')
              AND (:organizationId IS NULL OR i.organization_id = :organizationId)
            GROUP BY i.organization_id
            """, nativeQuery = true)
    List<Object[]> outstandingByOrganization(@Param("organizationId") UUID organizationId);

    /** The same outstanding figure grouped by vendor, as {@code [vendor_id, outstanding]} rows. */
    @Query(value = """
            SELECT i.vendor_id, COALESCE(SUM(i.total_amount - i.paid_amount), 0)
            FROM invoices i
            WHERE i.status IN ('APPROVED', 'PARTIALLY_PAID', 'OVERDUE')
              AND i.organization_id = :organizationId
            GROUP BY i.vendor_id
            ORDER BY 2 DESC
            """, nativeQuery = true)
    List<Object[]> outstandingByVendor(@Param("organizationId") UUID organizationId);
}
