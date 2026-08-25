package com.vendorsphere.vendor.dto;

import com.vendorsphere.vendor.VendorStatus;
import jakarta.validation.constraints.NotNull;

public record VendorStatusChangeRequest(
        @NotNull VendorStatus status,
        String reason
) {
}
