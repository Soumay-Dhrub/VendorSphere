package com.vendorsphere.payment.repository;

import com.vendorsphere.invoice.InvoiceStatus;
import com.vendorsphere.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByInvoiceIdOrderByCreatedAtAscIdAsc(UUID invoiceId);

    @Query(value = """
            SELECT i.organization_id, COALESCE(SUM(i.total_amount - i.paid_amount), 0)
            FROM invoices i
            WHERE i.status IN ('APPROVED', 'PARTIALLY_PAID', 'OVERDUE')
              AND (:organizationId IS NULL OR i.organization_id = :organizationId)
            GROUP BY i.organization_id
            """, nativeQuery = true)
    List<Object[]> outstandingByOrganization(@Param("organizationId") UUID organizationId);

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
