package com.vendorsphere.quotation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Submission and revision payload (Requirements 12.4, 12.8). It carries vendor-supplied primitives
 * only - no computed totals exist here, so the platform figures cannot be manipulated by a client
 * (Requirement 13.6).
 */
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

    /** One priced line answering one RFQ item. */
    public record ItemLine(
            @NotNull UUID rfqItemId,
            @NotNull BigDecimal quantity,
            @NotNull BigDecimal unitPrice,
            BigDecimal taxRate,
            BigDecimal discountAmount
    ) {
    }
}
