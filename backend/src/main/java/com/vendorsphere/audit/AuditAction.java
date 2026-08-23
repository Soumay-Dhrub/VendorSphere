package com.vendorsphere.audit;

/**
 * The state-changing operations that carry an audit log entry.
 *
 * <p>The twenty-two constants below are exactly the operations listed by Requirement 29.2, in the
 * order that clause names them. Persisted as {@code VARCHAR(100)} through
 * {@code @Enumerated(EnumType.STRING)} into {@code audit_logs.action}.
 *
 * <p>Adding a constant here is not enough to produce an entry: the owning service must call
 * {@link com.vendorsphere.audit.service.AuditService#record}. Each module task does that wiring.
 */
public enum AuditAction {

    /** Requirement 2.1: a vendor was registered. */
    VENDOR_CREATED,

    /** Requirement 2.4: a vendor profile was updated. */
    VENDOR_UPDATED,

    /** Requirement 3.3: a vendor status changed, carrying previous status, new status and reason. */
    VENDOR_STATUS_CHANGED,

    /** Requirement 8.1: a purchase request moved from DRAFT to SUBMITTED. */
    PURCHASE_REQUEST_SUBMITTED,

    /** Requirement 8.5: a purchase request was approved. */
    PURCHASE_REQUEST_APPROVED,

    /** Requirement 8.7: a purchase request was rejected. */
    PURCHASE_REQUEST_REJECTED,

    /** Requirement 9.1: an RFQ was created from a purchase request. */
    RFQ_CREATED,

    /** Requirement 11.1: an RFQ status changed. */
    RFQ_STATUS_CHANGED,

    /** Requirement 11.7: an RFQ was cancelled with a reason. */
    RFQ_CANCELLED,

    /** Requirement 10.1: one or more vendors were invited to an RFQ. */
    VENDOR_INVITED,

    /** Requirement 12.7: a vendor submitted a quotation. */
    QUOTATION_SUBMITTED,

    /** Requirement 12.8: a vendor revised a submitted quotation. */
    QUOTATION_REVISED,

    /** Requirement 17.1: a quotation was awarded. */
    VENDOR_SELECTED,

    /** Requirement 18.1: a purchase order was generated from an award. */
    PURCHASE_ORDER_GENERATED,

    /** Requirement 19.3: a purchase order was issued to the vendor. */
    PURCHASE_ORDER_ISSUED,

    /** Requirement 19.6: a purchase order was cancelled. */
    PURCHASE_ORDER_CANCELLED,

    /** Requirement 20.1: a goods receipt was recorded against a purchase order. */
    DELIVERY_RECORDED,

    /** Requirement 22.1: a vendor submitted an invoice. */
    INVOICE_SUBMITTED,

    /** Requirement 24.1: an invoice was approved. */
    INVOICE_APPROVED,

    /** Requirement 24.1: an invoice was rejected. */
    INVOICE_REJECTED,

    /** Requirement 24.4: a three-way match finding was overridden with a justification. */
    MATCH_FINDING_OVERRIDDEN,

    /** Requirement 25.1: a payment was recorded against an invoice. */
    PAYMENT_RECORDED
}
