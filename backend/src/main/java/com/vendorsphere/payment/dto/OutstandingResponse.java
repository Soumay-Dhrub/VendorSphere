package com.vendorsphere.payment.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** Outstanding payables: the total and the per-vendor breakdown (Requirement 25.10). */
public record OutstandingResponse(
        BigDecimal totalOutstanding,
        Map<UUID, BigDecimal> outstandingByVendor
) {
}
