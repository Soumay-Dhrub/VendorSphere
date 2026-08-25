package com.vendorsphere.procurement.dto;

import com.vendorsphere.procurement.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Create and update payload for a purchase request header (Requirement 7.1).
 *
 * <p>{@code priority} is optional on purpose: Requirement 7.2 defaults it to MEDIUM when absent.
 * {@code estimatedBudget} maps a {@code DECIMAL(15, 2)} column and is normalized to money scale by
 * the service; it is optional because not every requirement carries a budget figure yet.
 *
 * <p>The record deliberately carries no status, requester or request number: all three are derived
 * server-side, so a client cannot set or forge them (Requirement 7.1).
 */
public record PurchaseRequestHeaderRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull UUID departmentId,
        String businessJustification,
        LocalDate requiredDate,
        Priority priority,
        BigDecimal estimatedBudget
) {
}
