package com.vendorsphere.vendor.dto;

import com.vendorsphere.vendor.VendorStatus;
import com.vendorsphere.vendor.entity.Vendor;
import com.vendorsphere.vendor.entity.VendorCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A vendor profile together with the three figures Requirement 2.5 asks a detail read to carry: the
 * vendor category name, the current performance score and the count of documents whose expiry date
 * falls within 30 days of the request date.
 *
 * <p>{@code categoryId}, {@code categoryName} and the two derived values are all nullable-safe: a
 * vendor without a category reports {@code null} for both category fields, and both numeric values
 * are normalized at money scale so they serialize with exactly two decimals (Requirement 32.7).
 */
public record VendorResponse(
        UUID id,
        String vendorCode,
        String companyName,
        String contactPerson,
        String email,
        String phone,
        String address,
        String taxIdentifier,
        VendorStatus status,
        BigDecimal rating,
        UUID categoryId,
        String categoryName,
        BigDecimal performanceScore,
        long expiringDocumentCount,
        Instant registeredAt,
        Instant createdAt
) {

    /**
     * Projects a vendor and its derived figures.
     *
     * <p>The caller supplies {@code performanceScore} and {@code expiringDocumentCount} because both
     * are queried rather than mapped: keeping them as parameters stops this record from reaching
     * into repositories.
     */
    public static VendorResponse from(
            Vendor vendor,
            BigDecimal performanceScore,
            long expiringDocumentCount) {
        VendorCategory category = vendor.getCategory();
        return new VendorResponse(
                vendor.getId(),
                vendor.getVendorCode(),
                vendor.getCompanyName(),
                vendor.getContactPerson(),
                vendor.getEmail(),
                vendor.getPhone(),
                vendor.getAddress(),
                vendor.getTaxIdentifier(),
                vendor.getStatus(),
                vendor.getRating(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                performanceScore,
                expiringDocumentCount,
                vendor.getRegisteredAt(),
                vendor.getCreatedAt());
    }
}
