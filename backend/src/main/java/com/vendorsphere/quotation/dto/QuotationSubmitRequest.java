package com.vendorsphere.quotation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record QuotationSubmitRequest(
        @NotEmpty List<ItemLine> items,
        BigDecimal shippingAmount,
        Integer deliveryPeriodDays,
        String paymentTerms,
        String warranty,
        Integer warrantyMonths,
        LocalDate validityDate,
        String notes
) {

    public record ItemLine(
            @NotNull UUID rfqItemId,
            @NotNull BigDecimal quantity,
            @NotNull BigDecimal unitPrice,
            BigDecimal taxRate,
            BigDecimal discountAmount
    ) {
    }
}
