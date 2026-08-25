package com.vendorsphere.procurement.repository;

import com.vendorsphere.procurement.entity.PurchaseRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseRequestItemRepository extends JpaRepository<PurchaseRequestItem, UUID> {

    Optional<PurchaseRequestItem> findByIdAndPurchaseRequestOrganizationId(
            UUID id, UUID organizationId);

    long countByPurchaseRequestId(UUID purchaseRequestId);

    List<PurchaseRequestItem> findByPurchaseRequestIdOrderBySortOrderAscIdAsc(UUID purchaseRequestId);

    List<PurchaseRequestItem> findByPurchaseRequestIdInOrderBySortOrderAscIdAsc(
            Collection<UUID> purchaseRequestIds);

    default Map<UUID, List<PurchaseRequestItem>> itemsByPurchaseRequestId(
            Collection<UUID> purchaseRequestIds) {
        if (purchaseRequestIds == null || purchaseRequestIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<PurchaseRequestItem>> result = new HashMap<>();
        for (PurchaseRequestItem item : findByPurchaseRequestIdInOrderBySortOrderAscIdAsc(purchaseRequestIds)) {
            result.computeIfAbsent(item.getPurchaseRequest().getId(), id -> new ArrayList<>())
                    .add(item);
        }
        return result;
    }
}
