package com.vendorsphere.payment.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRecordRequest(
        @NotNull BigDecimal amount,
        @NotNull LocalDate paymentDate,
        String paymentReference,
        String paymentMethod,
        String notes
) {
}
