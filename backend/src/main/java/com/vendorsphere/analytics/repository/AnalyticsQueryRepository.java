package com.vendorsphere.analytics.repository;

import com.vendorsphere.purchaseorder.entity.PurchaseOrder;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface AnalyticsQueryRepository extends Repository<PurchaseOrder, UUID> {

    @Query(value = """
            SELECT COUNT(d.id),
                   COALESCE(SUM(CASE WHEN d.delivery_date <= po.expected_delivery
                                     THEN 1 ELSE 0 END), 0)
            FROM deliveries d
            JOIN purchase_orders po ON po.id = d.purchase_order_id
            WHERE po.vendor_id = :vendorId AND po.status <> 'CANCELLED'
            """, nativeQuery = true)
    List<Object[]> deliveryCounts(@Param("vendorId") UUID vendorId);

    @Query(value = """
            SELECT COALESCE(SUM(di.received_quantity), 0),
                   COALESCE(SUM(di.damaged_quantity + di.rejected_quantity), 0)
            FROM delivery_items di
            JOIN deliveries d ON d.id = di.delivery_id
            JOIN purchase_orders po ON po.id = d.purchase_order_id
            WHERE po.vendor_id = :vendorId
            """, nativeQuery = true)
    List<Object[]> qualityQuantities(@Param("vendorId") UUID vendorId);

    @Query(value = """
            SELECT AVG(peer.mean_total / q.total_amount)
            FROM quotations q
            JOIN (SELECT rfq_id, AVG(total_amount) AS mean_total
                  FROM quotations GROUP BY rfq_id) peer ON peer.rfq_id = q.rfq_id
            WHERE q.vendor_id = :vendorId AND q.total_amount > 0
            """, nativeQuery = true)
    List<BigDecimal> pricingRatioMean(@Param("vendorId") UUID vendorId);

    @Query(value = """
            SELECT (SELECT COUNT(*) FROM rfq_vendors rv WHERE rv.vendor_id = :vendorId),
                   (SELECT COUNT(*)
                    FROM quotations q JOIN rfqs r ON r.id = q.rfq_id
                    WHERE q.vendor_id = :vendorId
                      AND q.status <> 'DRAFT'
                      AND q.submitted_at < r.closing_date)
            """, nativeQuery = true)
    List<Object[]> responsivenessCounts(@Param("vendorId") UUID vendorId);

    @Query(value = """
            SELECT COUNT(*),
                   COALESCE(SUM(CASE WHEN po.status IN ('DELIVERED', 'CLOSED')
                                     THEN 1 ELSE 0 END), 0)
            FROM purchase_orders po
            WHERE po.vendor_id = :vendorId AND po.status NOT IN ('DRAFT', 'CANCELLED')
            """, nativeQuery = true)
    List<Object[]> fulfilmentCounts(@Param("vendorId") UUID vendorId);

    // ----- dashboard (Requirement 27) -----

    @Query(value = """
            SELECT COALESCE(SUM(total_amount), 0) FROM purchase_orders
            WHERE organization_id = :organizationId
              AND status NOT IN ('DRAFT', 'CANCELLED')
            """, nativeQuery = true)
    BigDecimal totalSpend(@Param("organizationId") UUID organizationId);

    @Query(value = """
            SELECT COUNT(*) FROM rfqs
            WHERE organization_id = :organizationId AND status IN ('OPEN', 'EVALUATION')
            """, nativeQuery = true)
    long activeRfqCount(@Param("organizationId") UUID organizationId);

    @Query(value = """
            SELECT COUNT(*) FROM purchase_orders
            WHERE organization_id = :organizationId
              AND status IN ('ISSUED', 'ACKNOWLEDGED', 'PARTIALLY_DELIVERED')
            """, nativeQuery = true)
    long openPurchaseOrderCount(@Param("organizationId") UUID organizationId);

    @Query(value = """
            SELECT COUNT(*) FROM purchase_orders
            WHERE organization_id = :organizationId AND delivery_overdue = TRUE
              AND status IN ('ISSUED', 'ACKNOWLEDGED')
            """, nativeQuery = true)
    long pendingDeliveryCount(@Param("organizationId") UUID organizationId);

    @Query(value = "SELECT COUNT(*) FROM vendors WHERE organization_id = :organizationId "
            + "AND status = 'ACTIVE'", nativeQuery = true)
    long activeVendorCount(@Param("organizationId") UUID organizationId);

    @Query(value = """
            SELECT COUNT(*) FROM invoices WHERE organization_id = :organizationId
              AND status IN ('APPROVED', 'PARTIALLY_PAID')
            """, nativeQuery = true)
    long outstandingInvoiceCount(@Param("organizationId") UUID organizationId);

    @Query(value = "SELECT COUNT(*) FROM invoices WHERE organization_id = :organizationId "
            + "AND status = 'OVERDUE'", nativeQuery = true)
    long overdueInvoiceCount(@Param("organizationId") UUID organizationId);

    @Query(value = """
            SELECT COALESCE(SUM(total_amount - paid_amount), 0) FROM invoices
            WHERE organization_id = :organizationId
              AND status IN ('APPROVED', 'PARTIALLY_PAID', 'OVERDUE')
            """, nativeQuery = true)
    BigDecimal outstandingPayables(@Param("organizationId") UUID organizationId);
}
