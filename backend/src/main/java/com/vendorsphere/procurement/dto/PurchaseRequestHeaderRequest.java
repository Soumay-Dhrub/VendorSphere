package com.vendorsphere.procurement.dto;

import com.vendorsphere.procurement.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PurchaseRequestHeaderRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull UUID departmentId,
        String businessJustification,
        LocalDate requiredDate,
        Priority priority,
        BigDecimal estimatedBudget
) {
}
