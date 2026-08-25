package com.vendorsphere.vendor.dto;

import com.vendorsphere.vendor.entity.VendorCategory;

import java.time.Instant;
import java.util.UUID;

public record VendorCategoryResponse(
        UUID id,
        String name,
        String description,
        Instant createdAt
) {

    public static VendorCategoryResponse from(VendorCategory category) {
        return new VendorCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getCreatedAt());
    }
}
