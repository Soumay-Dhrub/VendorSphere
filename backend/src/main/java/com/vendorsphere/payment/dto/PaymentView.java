package com.vendorsphere.payment.dto;

import com.vendorsphere.common.util.Money;
import com.vendorsphere.payment.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** One recorded payment as listed by GET /payments. */
public record PaymentView(
        UUID id,
        UUID invoiceId,
        BigDecimal amount,
        LocalDate paymentDate,
        String paymentReference,
        String paymentMethod
) {

    public static PaymentView from(Payment payment) {
        return new PaymentView(
                payment.getId(),
                payment.getInvoice().getId(),
                Money.money(payment.getAmount()),
                payment.getPaymentDate(),
                payment.getPaymentReference(),
                payment.getPaymentMethod());
    }
}
