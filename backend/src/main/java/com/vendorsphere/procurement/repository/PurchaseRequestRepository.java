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

public interface PurchaseRequestRepository
        extends JpaRepository<PurchaseRequest, UUID>, JpaSpecificationExecutor<PurchaseRequest> {

    Optional<PurchaseRequest> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query(value = """
            SELECT r.purchase_request_id, r.id
            FROM rfqs r
            WHERE r.purchase_request_id IN (:purchaseRequestIds)
            ORDER BY r.created_at
            """, nativeQuery = true)
    List<Object[]> findRfqIdsByPurchaseRequestIds(
            @Param("purchaseRequestIds") Collection<UUID> purchaseRequestIds);

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
