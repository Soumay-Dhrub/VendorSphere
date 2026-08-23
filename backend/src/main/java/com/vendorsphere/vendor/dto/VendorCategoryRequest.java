package com.vendorsphere.vendor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create and update payload for a vendor category (Requirement 4.4).
 *
 * <p>{@code name} is mandatory and capped at the {@code VARCHAR(255)} width of
 * {@code vendor_categories.name}; {@code description} maps to a {@code TEXT} column and is therefore
 * unbounded and optional.
 *
 * <p>The record carries no organization identifier: the category belongs to the Actor's organization
 * by construction, so a client cannot file one under another tenant.
 */
public record VendorCategoryRequest(
        @NotBlank @Size(max = 255) String name,
        String description
) {
}
