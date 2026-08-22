package com.vendorsphere.invoice;

/**
 * Lifecycle status of a vendor invoice.
 *
 * <p>Persisted as {@code VARCHAR} through {@code @Enumerated(EnumType.STRING)}, matching the
 * {@code chk_invoice_status} constraint on {@code invoices.status}. Permitted transitions are held by
 * {@link InvoiceStatusTransitions}.
 */
public enum InvoiceStatus {

    /** Received from the vendor and awaiting finance review. */
    SUBMITTED,

    /** Being reviewed by finance. */
    UNDER_REVIEW,

    /** Approved for payment. */
    APPROVED,

    /** Declined with a recorded reason. */
    REJECTED,

    /** Settled in part. */
    PARTIALLY_PAID,

    /** Settled in full. */
    PAID,

    /** Unpaid past its due date. */
    OVERDUE
}
