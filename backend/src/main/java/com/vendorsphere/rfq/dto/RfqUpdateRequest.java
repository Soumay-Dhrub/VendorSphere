package com.vendorsphere.rfq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record RfqUpdateRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotNull Instant openingDate,
        @NotNull Instant closingDate,
        @Size(max = 3) String currency,
        String deliveryLocation,
        String terms
) {
}
