package com.vendorsphere.vendor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Registration and update payload for a vendor profile (Requirements 2.1, 2.4).
 *
 * <p>{@code companyName} and {@code email} are mandatory on both operations, so an omitted value
 * fails bean validation and the global exception handler answers 400 with a field-level message
 * (Requirement 2.2). {@code Size} limits mirror the column widths of {@code vendors} so an
 * over-long value is reported as a validation error rather than a database constraint violation.
 *
 * <p>The record deliberately carries no status, rating, vendor code or registration timestamp: all
 * four are derived server-side, so a client cannot set or change them (Requirements 2.1, 2.4).
 */
public record VendorRequest(
        @NotBlank @Size(max = 255) String companyName,
        @Size(max = 255) String contactPerson,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 30) String phone,
        String address,
        @Size(max = 100) String taxIdentifier,
        UUID categoryId
) {
}
