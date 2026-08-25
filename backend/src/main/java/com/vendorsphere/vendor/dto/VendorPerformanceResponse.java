package com.vendorsphere.vendor.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record VendorPerformanceResponse(
        UUID vendorId,
        BigDecimal overallScore,
        BigDecimal vendorRating
) {

    public static VendorPerformanceResponse from(
            UUID vendorId, BigDecimal overallScore, BigDecimal vendorRating) {
        return new VendorPerformanceResponse(vendorId, overallScore, vendorRating);
    }
}
