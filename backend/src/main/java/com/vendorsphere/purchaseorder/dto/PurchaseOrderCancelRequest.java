package com.vendorsphere.purchaseorder.dto;

/** Cancellation payload; the reason is mandatory (Requirement 19.7). */
public record PurchaseOrderCancelRequest(String reason) {
}
