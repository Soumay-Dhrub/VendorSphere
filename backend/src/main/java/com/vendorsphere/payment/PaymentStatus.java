package com.vendorsphere.payment;

/**
 * Status of a recorded payment against an invoice.
 *
 * <p>Persisted as {@code VARCHAR} through {@code @Enumerated(EnumType.STRING)}, matching the
 * {@code chk_payment_status} constraint on {@code payments.status}.
 */
public enum PaymentStatus {

    /** Recorded but not yet settled. */
    PENDING,

    /** Settles part of the invoice total. */
    PARTIALLY_PAID,

    /** Settled. Only payments in this status contribute to the invoice paid amount. */
    PAID,

    /** Settlement failed. */
    FAILED
}
