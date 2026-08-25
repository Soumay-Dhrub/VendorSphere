package com.vendorsphere.quotation.dto;

import com.vendorsphere.rfq.RfqStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The normalized side-by-side comparison of an RFQ (Requirement 15): one row per qualifying
 * quotation, ordered by evaluation score descending then total amount ascending.
 */
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

    /** The quoted figures of one RFQ item within a row (Requirement 15.3). */
    public record ItemRow(
            UUID rfqItemId,
            String itemName,
            BigDecimal rfqQuantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }
}
