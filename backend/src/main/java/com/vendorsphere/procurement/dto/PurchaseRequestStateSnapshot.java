package com.vendorsphere.procurement.dto;

import com.vendorsphere.procurement.PurchaseRequestStatus;

import java.util.UUID;

/**
 * The audited state of a purchase request at one moment, the previous and new value of the
 * {@code PURCHASE_REQUEST_SUBMITTED}, {@code PURCHASE_REQUEST_APPROVED} and
 * {@code PURCHASE_REQUEST_REJECTED} trail entries (Requirement 29.2).
 *
 * <p>A purpose-built narrow record rather than the entity, for the same reason the vendor snapshots
 * are: serializing an entity would drag lazy associations into {@code audit_logs}. The review notes
 * ride along because a rejection's reason is part of what makes the decision traceable.
 */
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
