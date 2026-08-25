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

/**
 * Item reads for purchase requests. Items have no organization column of their own, so every
 * tenant-scoped finder traverses {@code purchase_request.organization.id}; a cross-tenant identifier
 * misses and surfaces as 404 (Requirement 30.10).
 */
public interface PurchaseRequestItemRepository extends JpaRepository<PurchaseRequestItem, UUID> {

    /** The single item of one request, reachable only through that request's organization. */
    Optional<PurchaseRequestItem> findByIdAndPurchaseRequestOrganizationId(
            UUID id, UUID organizationId);

    long countByPurchaseRequestId(UUID purchaseRequestId);

    List<PurchaseRequestItem> findByPurchaseRequestIdOrderBySortOrderAscIdAsc(UUID purchaseRequestId);

    /**
     * Every item of the supplied requests, authoring order preserved, as one query.
     *
     * <p>A request list page shows its lines, so reading them per request would issue one query per
     * row and make a page's cost grow with its size. This runs once for the whole page instead
     * (Requirement 31.2). Prefer {@link #itemsByPurchaseRequestId(Collection)} over calling this
     * directly.
     */
    List<PurchaseRequestItem> findByPurchaseRequestIdInOrderBySortOrderAscIdAsc(
            Collection<UUID> purchaseRequestIds);

    /**
     * {@link #findByPurchaseRequestIdInOrderBySortOrderAscIdAsc(Collection)} grouped by parent
     * identifier, with an empty input short-circuited so an empty page issues no query at all.
     * Requests with no items map to an empty list rather than being absent.
     *
     * <p>Reading the parent's identifier goes through the lazy proxy without a query - the id is part
     * of the foreign key already loaded on the item row.
     */
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
