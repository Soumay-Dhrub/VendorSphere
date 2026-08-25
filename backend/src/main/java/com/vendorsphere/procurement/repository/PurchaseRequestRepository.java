package com.vendorsphere.procurement.repository;

import com.vendorsphere.procurement.entity.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Every finder is keyed on the organization, so a cross-tenant identifier misses and surfaces as 404
 * rather than 403 (Requirement 30.10).
 *
 * <p>{@link JpaSpecificationExecutor} backs the paginated listing of Requirement 31.1, whose
 * predicates live in {@link PurchaseRequestSpecifications}. The organization predicate is part of
 * that specification, so {@code findAll(Specification, Pageable)} is only ever called with it.
 */
public interface PurchaseRequestRepository
        extends JpaRepository<PurchaseRequest, UUID>, JpaSpecificationExecutor<PurchaseRequest> {

    Optional<PurchaseRequest> findByIdAndOrganizationId(UUID id, UUID organizationId);

    /**
     * Identifiers of the RFQs sourced from each of the supplied purchase requests, as
     * {@code [purchase_request_id, rfq_id]} rows.
     *
     * <p>Requirement 8.9 asks a detail read to report which RFQs were derived from a request. The
     * {@code rfqs} table exists from V1 but its entity and repository belong to the RFQ module, so -
     * exactly like the performance-snapshot reads in {@code VendorRepository} - this is a native
     * projection over that table rather than a second mapping of it. Once the RFQ module lands the
     * query can move onto its repository unchanged.
     *
     * <p>Batched rather than per-request because a list page carries the same derived figure per row;
     * one grouped round trip keeps the page's cost fixed whatever its size (Requirement 31.2).
     *
     * <p>Prefer {@link #rfqIdsByPurchaseRequestId(Collection)} over calling this directly.
     */
    @Query(value = """
            SELECT r.purchase_request_id, r.id
            FROM rfqs r
            WHERE r.purchase_request_id IN (:purchaseRequestIds)
            ORDER BY r.created_at
            """, nativeQuery = true)
    List<Object[]> findRfqIdsByPurchaseRequestIds(
            @Param("purchaseRequestIds") Collection<UUID> purchaseRequestIds);

    /**
     * {@link #findRfqIdsByPurchaseRequestIds(Collection)} keyed by purchase request identifier, with
     * an empty input short-circuited so an empty page issues no query at all. Requests with no
     * derived RFQ map to an empty list rather than being absent.
     */
    default Map<UUID, List<UUID>> rfqIdsByPurchaseRequestId(Collection<UUID> purchaseRequestIds) {
        if (purchaseRequestIds == null || purchaseRequestIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<UUID>> result = new HashMap<>();
        for (Object[] row : findRfqIdsByPurchaseRequestIds(purchaseRequestIds)) {
            result.computeIfAbsent((UUID) row[0], id -> new java.util.ArrayList<>())
                    .add((UUID) row[1]);
        }
        return result;
    }
}
