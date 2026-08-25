package com.vendorsphere.purchaseorder.repository;

import com.vendorsphere.purchaseorder.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, UUID> {

    List<PurchaseOrderItem> findByPurchaseOrderIdOrderByCreatedAtAscIdAsc(UUID purchaseOrderId);

    Optional<PurchaseOrderItem> findByIdAndPurchaseOrderOrganizationId(
            UUID id, UUID organizationId);
}
