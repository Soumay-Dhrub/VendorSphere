package com.vendorsphere.invoice.repository;

import com.vendorsphere.invoice.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Invoice item reads. */
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, UUID> {

    List<InvoiceItem> findByInvoiceIdOrderByCreatedAtAscIdAsc(UUID invoiceId);
}
