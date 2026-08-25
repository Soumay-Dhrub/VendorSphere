package com.vendorsphere.vendor.dto;

import com.vendorsphere.vendor.VendorStatus;
import com.vendorsphere.vendor.entity.Vendor;
import com.vendorsphere.vendor.entity.VendorCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
