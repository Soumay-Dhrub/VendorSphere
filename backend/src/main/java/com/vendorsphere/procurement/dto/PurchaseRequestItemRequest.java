package com.vendorsphere.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Create and update payload for one purchase request item (Requirement 7.4).
 *
 * <p>{@code quantity} is a {@code BigDecimal} so a client cannot lose precision on a fractional
 * quantity; positivity and quantity-scale normalization are the service's rule, because bean
 * validation cannot express {@code > 0} with the pinned message of Requirement 7.5.
 *
 * <p>{@code unit} is optional: an absent or blank value stores as the column default UNIT.
 */
public record PurchaseRequestItemRequest(
        @NotBlank @Size(max = 255) String itemName,
        @NotNull BigDecimal quantity,
        @Size(max = 50) String unit,
        String specification
) {
}
