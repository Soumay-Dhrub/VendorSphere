package com.vendorsphere.rfq.repository;

import com.vendorsphere.rfq.entity.RfqItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Item reads for RFQs. Items have no organization column of their own, so every tenant-scoped finder
 * traverses {@code rfq.organization.id}; a cross-tenant identifier misses as 404 (Requirement 30.10).
 */
public interface RfqItemRepository extends JpaRepository<RfqItem, UUID> {

    Optional<RfqItem> findByIdAndRfqOrganizationId(UUID id, UUID organizationId);

    long countByRfqId(UUID rfqId);

    List<RfqItem> findByRfqIdOrderBySortOrderAscIdAsc(UUID rfqId);
}
