package com.vendorsphere.rfq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RfqItemRequest(
        @NotBlank @Size(max = 255) String itemName,
        @NotNull BigDecimal quantity,
        @Size(max = 50) String unit,
        String specification
) {
}
