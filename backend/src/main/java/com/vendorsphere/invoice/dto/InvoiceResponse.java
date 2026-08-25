package com.vendorsphere.invoice.dto;

import com.vendorsphere.invoice.InvoiceStatus;
import com.vendorsphere.invoice.MatchFindingType;
import com.vendorsphere.invoice.MatchResolutionState;
import com.vendorsphere.invoice.MatchStatus;
import com.vendorsphere.invoice.entity.Invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID purchaseOrderId,
        UUID vendorId,
        String invoiceNumber,
        LocalDate invoiceDate,
        LocalDate dueDate,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        InvoiceStatus status,
        MatchStatus matchStatus,
        Instant reviewedAt,
        List<ItemResponse> items
) {

    public record ItemResponse(
            UUID id,
            UUID sourcePoItemId,
            String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal taxAmount,
            BigDecimal lineTotal
    ) {
    }

    /** Compact line shape used for duplicate-line comparison and match results. */
    public record ItemResponse2(UUID sourcePoItemId, BigDecimal quantity, BigDecimal unitPrice) {
    }

    public record FindingResponse(
            UUID findingId,
            MatchFindingType type,
            String itemName,
            String expectedValue,
            String actualValue,
            String detail,
            MatchResolutionState resolutionState,
            String overrideJustification
    ) {
    }

    public record MatchItem(
            UUID invoiceItemId,
            String itemName,
            BigDecimal orderedQuantity,
            BigDecimal receivedQuantity,
            BigDecimal invoicedQuantity,
            BigDecimal poUnitPrice,
            BigDecimal invoicedUnitPrice
    ) {
    }

    public record MatchResult(
            UUID invoiceId,
            MatchStatus matchStatus,
            List<FindingResponse> findings,
            List<MatchItem> items
    ) {
    }

    public static InvoiceResponse from(Invoice invoice, List<ItemResponse> items) {
        return new InvoiceResponse(invoice.getId(),
                invoice.getPurchaseOrder().getId(), invoice.getVendor().getId(),
                invoice.getInvoiceNumber(), invoice.getInvoiceDate(), invoice.getDueDate(),
                invoice.getSubtotal(), invoice.getTaxAmount(), invoice.getDiscountAmount(),
                invoice.getTotalAmount(), invoice.getPaidAmount(), invoice.getStatus(),
                invoice.getMatchStatus(), invoice.getReviewedAt(), items);
    }
}
