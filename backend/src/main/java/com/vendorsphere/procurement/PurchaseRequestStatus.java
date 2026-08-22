package com.vendorsphere.procurement;

/**
 * Lifecycle status of a purchase request.
 *
 * <p>Persisted as {@code VARCHAR} through {@code @Enumerated(EnumType.STRING)}, matching the
 * {@code chk_pr_status} constraint on {@code purchase_requests.status}. Permitted transitions are
 * held by {@link PurchaseRequestStatusTransitions}.
 */
public enum PurchaseRequestStatus {

    /** Being authored; items and attachments may still be changed. */
    DRAFT,

    /** Submitted for review; items are locked. */
    SUBMITTED,

    /** Picked up by a reviewer. */
    UNDER_REVIEW,

    /** Approved for procurement. */
    APPROVED,

    /** Declined with a recorded reason. */
    REJECTED,

    /** At least one RFQ has been raised from the request. */
    PROCUREMENT_STARTED,

    /** Procurement for the request has finished. */
    COMPLETED
}
