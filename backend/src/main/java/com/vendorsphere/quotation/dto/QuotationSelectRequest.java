package com.vendorsphere.quotation.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Award payload (Requirement 17): the winning quotation and a mandatory justification. */
public record QuotationSelectRequest(
        @NotNull UUID quotationId,
        String justification
) {
}
