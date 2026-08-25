package com.vendorsphere.vendor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorCategoryRequest(
        @NotBlank @Size(max = 255) String name,
        String description
) {
}
