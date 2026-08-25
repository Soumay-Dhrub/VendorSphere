package com.vendorsphere.delivery.repository;

import com.vendorsphere.delivery.entity.DeliveryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryItemRepository extends JpaRepository<DeliveryItem, UUID> {

    List<DeliveryItem> findByDeliveryId(UUID deliveryId);
}
