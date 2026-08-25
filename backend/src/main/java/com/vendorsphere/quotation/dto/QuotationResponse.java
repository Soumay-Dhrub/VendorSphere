package com.vendorsphere.quotation.dto;

import com.vendorsphere.quotation.QuotationStatus;
import com.vendorsphere.quotation.entity.Quotation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record QuotationResponse(
        UUID id,
        UUID rfqId,
        UUID vendorId,
        String quotationNumber,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal shippingAmount,
        BigDecimal totalAmount,
        Integer deliveryPeriodDays,
        String paymentTerms,
        String warranty,
        Integer warrantyMonths,
        LocalDate validityDate,
        String notes,
        QuotationStatus status,
        Instant submittedAt,
        List<ItemResponse> items
) {

    public record ItemResponse(
            UUID id,
            UUID rfqItemId,
            String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            BigDecimal discountAmount,
            BigDecimal lineTotal
    ) {

        public static ItemResponse from(com.vendorsphere.quotation.entity.QuotationItem item) {
            return new ItemResponse(
                    item.getId(),
                    item.getSourceItem() != null ? item.getSourceItem().getId() : null,
                    item.getItemName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getTaxRate(),
                    item.getTaxAmount(),
                    item.getDiscountAmount(),
                    item.getLineTotal());
        }
    }

    public static QuotationResponse from(Quotation quotation, List<ItemResponse> items) {
        return new QuotationResponse(
                quotation.getId(),
                quotation.getRfq().getId(),
                quotation.getVendor().getId(),
                quotation.getQuotationNumber(),
                quotation.getSubtotal(),
                quotation.getTaxAmount(),
                quotation.getDiscountAmount(),
                quotation.getShippingAmount(),
                quotation.getTotalAmount(),
                quotation.getDeliveryPeriodDays(),
                quotation.getPaymentTerms(),
                quotation.getWarranty(),
                quotation.getWarrantyMonths(),
                quotation.getValidityDate(),
                quotation.getNotes(),
                quotation.getStatus(),
                quotation.getSubmittedAt(),
                items);
    }

    public static QuotationResponse redacted(Quotation quotation) {
        return new QuotationResponse(
                quotation.getId(),
                quotation.getRfq().getId(),
                quotation.getVendor().getId(),
                quotation.getQuotationNumber(),
                null, null, null, null, null,
                quotation.getDeliveryPeriodDays(),
                null, null, null, null, null,
                quotation.getStatus(),
                quotation.getSubmittedAt(),
                null);
    }
}
