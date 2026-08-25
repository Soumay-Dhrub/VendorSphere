package com.vendorsphere.quotation.repository;

import com.vendorsphere.quotation.entity.QuotationItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Item reads for quotations; items are reached only through their quotation's tenant scope. */
public interface QuotationItemRepository extends JpaRepository<QuotationItem, UUID> {

    List<QuotationItem> findByQuotationIdOrderByCreatedAtAscIdAsc(UUID quotationId);

    void deleteByQuotationId(UUID quotationId);
}
