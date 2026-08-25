package com.vendorsphere.purchaseorder.repository;

import com.vendorsphere.purchaseorder.PurchaseOrderStatus;
import com.vendorsphere.purchaseorder.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    Optional<PurchaseOrder> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<PurchaseOrder> findByIdAndOrganizationIdAndVendorId(
            UUID id, UUID organizationId, UUID vendorId);

    Optional<PurchaseOrder> findByRfqId(UUID rfqId);

    boolean existsByRfqId(UUID rfqId);

    List<PurchaseOrder> findByOrganizationId(UUID organizationId);

    java.util.List<PurchaseOrder> findByStatusInAndExpectedDeliveryBeforeAndDeliveryOverdueFalse(
            java.util.Collection<PurchaseOrderStatus> statuses, java.time.LocalDate cutoff);
}
