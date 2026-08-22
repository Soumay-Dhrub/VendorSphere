package com.vendorsphere.quotation;

/**
 * Lifecycle status of a vendor quotation.
 *
 * <p>Persisted as {@code VARCHAR} through {@code @Enumerated(EnumType.STRING)}, matching the
 * {@code chk_quotation_status} constraint on {@code quotations.status}.
 */
public enum QuotationStatus {

    /** Being prepared by the vendor and not yet submitted. */
    DRAFT,

    /** Submitted against an open RFQ. */
    SUBMITTED,

    /** Being compared and scored. */
    UNDER_REVIEW,

    /** Selected as the winning quotation. */
    SELECTED,

    /** Not selected, or rejected because the RFQ was cancelled. */
    REJECTED,

    /** Withdrawn by the submitting vendor. */
    WITHDRAWN
}
