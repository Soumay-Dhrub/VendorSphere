package com.vendorsphere.payment.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record OutstandingResponse(
        BigDecimal totalOutstanding,
        Map<UUID, BigDecimal> outstandingByVendor
) {
}
