package com.vendorsphere.purchaseorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** DRAFT-only header edit payload (Requirement 18.7). */
public record PurchaseOrderUpdateRequest(
        @NotBlank @Size(max = 1000) String deliveryAddress,
        LocalDate expectedDelivery,
        @Size(max = 2000) String paymentTerms,
        @Size(max = 5000) String termsConditions
) {
}
