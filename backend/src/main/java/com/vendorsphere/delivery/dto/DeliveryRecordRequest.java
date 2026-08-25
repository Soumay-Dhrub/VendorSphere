package com.vendorsphere.delivery.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DeliveryRecordRequest(
        @NotNull LocalDate deliveryDate,
        String notes,
        String proofDocumentUrl,
        @NotEmpty List<ItemLine> items
) {

    public record ItemLine(
            @NotNull UUID purchaseOrderItemId,
            @NotNull BigDecimal receivedQuantity,
            BigDecimal damagedQuantity,
            BigDecimal rejectedQuantity,
            String notes
    ) {
    }
}
