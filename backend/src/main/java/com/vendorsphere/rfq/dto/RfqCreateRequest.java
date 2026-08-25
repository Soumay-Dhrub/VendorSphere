package com.vendorsphere.rfq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Create payload for an RFQ raised from an approved purchase request (Requirement 9.1). The source
 * request must hold status APPROVED or PROCUREMENT_STARTED; anything else is a 409 naming the
 * current status (Requirement 9.2).
 */
public record RfqCreateRequest(
        @NotNull UUID purchaseRequestId,
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotNull Instant openingDate,
        @NotNull Instant closingDate,
        @Size(max = 3) String currency,
        String deliveryLocation,
        String terms
) {
}
