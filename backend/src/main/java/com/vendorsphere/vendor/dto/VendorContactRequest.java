package com.vendorsphere.vendor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create and update payload for one vendor contact (Requirements 4.1, 4.2).
 *
 * <p>{@code name} is the only mandatory field, matching {@code vendor_contacts.name NOT NULL}; the
 * {@code Size} limits mirror the remaining column widths so an over-long value is reported as a
 * field-level 400 rather than a database constraint violation.
 *
 * <p>{@code primaryContact} is a primitive, so an omitted JSON property reads as {@code false} — the
 * same default the column carries. A request therefore only ever asks to <em>become</em> the primary
 * contact; it never asks the vendor to be left without one, which is what Requirement 4.2 describes.
 */
public record VendorContactRequest(
        @NotBlank @Size(max = 255) String name,
        @Email @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @Size(max = 100) String designation,
        boolean primaryContact
) {
}
