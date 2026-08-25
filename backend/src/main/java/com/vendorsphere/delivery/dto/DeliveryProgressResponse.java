package com.vendorsphere.delivery.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DeliveryProgressResponse(
        UUID purchaseOrderId,
        List<ItemProgress> items
) {

    public record ItemProgress(
            UUID purchaseOrderItemId,
            String itemName,
            BigDecimal orderedQuantity,
            BigDecimal receivedQuantity,
            BigDecimal damagedQuantity,
            BigDecimal rejectedQuantity,
            BigDecimal outstandingQuantity
    ) {
    }
}
