package com.vendorsphere.purchaseorder.dto;

import com.vendorsphere.purchaseorder.PurchaseOrderStatus;
import com.vendorsphere.purchaseorder.entity.PurchaseOrder;
import com.vendorsphere.purchaseorder.entity.PurchaseOrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderResponse(
        UUID id,
        String purchaseOrderNumber,
        UUID rfqId,
        UUID quotationId,
        UUID vendorId,
        String vendorCompanyName,
        String deliveryAddress,
        LocalDate expectedDelivery,
        String paymentTerms,
        String termsConditions,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        PurchaseOrderStatus status,
        boolean deliveryOverdue,
        Instant issuedAt,
        Instant acknowledgedAt,
        Instant closedAt,
        List<ItemResponse> items
) {

    public record ItemResponse(
            UUID id,
            String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            BigDecimal lineTotal,
            BigDecimal deliveredQuantity
    ) {

        public static ItemResponse from(PurchaseOrderItem item) {
            return new ItemResponse(item.getId(), item.getItemName(), item.getQuantity(),
                    item.getUnitPrice(), item.getTaxRate(), item.getTaxAmount(),
                    item.getLineTotal(), item.getDeliveredQuantity());
        }
    }

    public static PurchaseOrderResponse from(PurchaseOrder po, List<ItemResponse> items) {
        return new PurchaseOrderResponse(po.getId(), po.getPoNumber(),
                po.getRfq() != null ? po.getRfq().getId() : null,
                po.getQuotation() != null ? po.getQuotation().getId() : null,
                po.getVendor().getId(), po.getVendor().getCompanyName(),
                po.getDeliveryAddress(), po.getExpectedDelivery(), po.getPaymentTerms(),
                po.getTermsConditions(), po.getSubtotal(), po.getTaxAmount(),
                po.getTotalAmount(), po.getStatus(), po.isDeliveryOverdue(),
                po.getIssuedAt(), po.getAcknowledgedAt(), po.getClosedAt(), items);
    }
}
