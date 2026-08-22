package com.vendorsphere.rfq;

/**
 * Lifecycle status of an RFQ.
 *
 * <p>Persisted as {@code VARCHAR} through {@code @Enumerated(EnumType.STRING)}, matching the
 * {@code chk_rfq_status} constraint on {@code rfqs.status}. Permitted transitions are held by
 * {@link RfqStatusTransitions}.
 */
public enum RfqStatus {

    /** Being prepared; header, items and invitations may still be changed. */
    DRAFT,

    /** Published; invited vendors may submit quotations until the closing date. */
    OPEN,

    /** The bidding window has ended; no further quotations are accepted. */
    CLOSED,

    /** Quotations are being compared and scored. */
    EVALUATION,

    /** A quotation has been explicitly selected. */
    AWARDED,

    /** Abandoned with a recorded reason. */
    CANCELLED
}
