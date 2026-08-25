package com.vendorsphere.vendor.dto;

import com.vendorsphere.vendor.VendorStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record VendorSearchCriteria(
        String companyName,
        UUID categoryId,
        VendorStatus status,
        BigDecimal minRating
) {

    public static VendorSearchCriteria none() {
        return new VendorSearchCriteria(null, null, null, null);
    }

    public String companyNameTerm() {
        return companyName == null || companyName.isBlank() ? null : companyName.trim();
    }
}
