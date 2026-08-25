package com.vendorsphere.quotation.dto;

import com.vendorsphere.rfq.RfqStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ComparisonResponse(
        UUID rfqId,
        String rfqNumber,
        RfqStatus rfqStatus,
        List<ComparisonRow> rows
) {

    public record ComparisonRow(
            UUID quotationId,
            UUID vendorId,
            String companyName,
            BigDecimal performanceScore,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal discountAmount,
            BigDecimal shippingAmount,
            BigDecimal totalAmount,
            Integer deliveryPeriodDays,
            Integer warrantyMonths,
            String paymentTerms,
            LocalDate validityDate,
            UUID evaluationId,
            BigDecimal evaluationScore,
            boolean recommended,
            List<ItemRow> items
    ) {
    }

    public record ItemRow(
            UUID rfqItemId,
            String itemName,
            BigDecimal rfqQuantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }
}
