package com.vendorsphere.invoice.repository;

import com.vendorsphere.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Tenant- and vendor-scoped invoice reads (Requirements 30.10, 14-analog). */
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Invoice> findByIdAndOrganizationIdAndVendorId(
            UUID id, UUID organizationId, UUID vendorId);

    List<Invoice> findByOrganizationId(UUID organizationId);

    List<Invoice> findByPurchaseOrderId(UUID purchaseOrderId);

    boolean existsByOrganizationIdAndVendorIdAndInvoiceNumber(
            UUID organizationId, UUID vendorId, String invoiceNumber);

    /** Unpaid invoices past their due date, for the daily overdue job (Requirement 24.9). */
    java.util.List<Invoice> findByDueDateBeforeAndStatusNotIn(
            java.time.LocalDate cutoff, java.util.Collection<com.vendorsphere.invoice.InvoiceStatus> statuses);
}
