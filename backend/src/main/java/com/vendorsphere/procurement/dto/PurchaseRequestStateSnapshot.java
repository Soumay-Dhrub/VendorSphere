package com.vendorsphere.procurement.dto;

import com.vendorsphere.procurement.PurchaseRequestStatus;

import java.util.UUID;

public record PurchaseRequestStateSnapshot(
        UUID purchaseRequestId,
        PurchaseRequestStatus status,
        String reviewNotes
) {

    public static PurchaseRequestStateSnapshot from(
            com.vendorsphere.procurement.entity.PurchaseRequest request) {
        return new PurchaseRequestStateSnapshot(
                request.getId(), request.getStatus(), request.getReviewNotes());
    }
}
