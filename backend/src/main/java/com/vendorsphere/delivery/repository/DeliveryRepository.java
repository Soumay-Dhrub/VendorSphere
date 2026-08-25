package com.vendorsphere.delivery.repository;

import com.vendorsphere.delivery.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    List<Delivery> findByPurchaseOrderIdOrderByDeliveryDateAscIdAsc(UUID purchaseOrderId);

    Optional<Delivery> findByIdAndPurchaseOrderOrganizationId(UUID id, UUID organizationId);

    boolean existsByPurchaseOrderId(UUID purchaseOrderId);
}
