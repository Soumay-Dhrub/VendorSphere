package com.vendorsphere.vendor.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The current performance standing of one vendor, as the {@code GET /vendors/{id}/performance}
 * endpoint reports it.
 *
 * <p>Both figures are the same ones a vendor detail read carries - the latest snapshot score, or the
 * rating-derived figure while no snapshot exists - exposed on their own path so callers can poll one
 * vendor's standing without loading its profile. The five-metric breakdown of Requirement 26 arrives
 * with the analytics module and will extend this response rather than replace these fields.
 */
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
