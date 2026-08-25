package com.vendorsphere.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceSubmitRequest(
        @NotBlank @Size(max = 100) String invoiceNumber,
        @NotNull LocalDate invoiceDate,
        LocalDate dueDate,
        BigDecimal discountAmount,
        String documentUrl,
        @NotEmpty List<ItemLine> items
) {

    public record ItemLine(
            @NotNull UUID purchaseOrderItemId,
            @NotNull BigDecimal quantity,
            @NotNull BigDecimal unitPrice,
            BigDecimal taxAmount
    ) {
    }
}
