package com.vendorsphere.vendor.dto;

import java.time.LocalDate;

/**
 * Upload payload for a vendor compliance document (Requirements 5.1 through 5.3).
 *
 * <p>{@code documentType} is a {@code String} rather than the {@link com.vendorsphere.vendor.VendorDocumentType}
 * enum on purpose: an unknown value must be rejected by the service with 400 and a message listing
 * the accepted types (Requirement 5.3), which a Jackson enum-deserialization failure at the message
 * converter could not express. The service is therefore the single place that decides what a valid
 * type is.
 *
 * <p>{@code expiryDate} is optional: documents such as bank details carry no expiry, and Requirement
 * 5.4 classifies an absent date as VALID.
 */
public record VendorDocumentRequest(
        String documentType,
        LocalDate expiryDate
) {
}
