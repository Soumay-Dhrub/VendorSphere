package com.vendorsphere.quotation.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record QuotationSelectRequest(
        @NotNull UUID quotationId,
        String justification
) {
}
