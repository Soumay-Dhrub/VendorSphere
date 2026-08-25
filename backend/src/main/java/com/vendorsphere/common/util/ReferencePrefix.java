package com.vendorsphere.common.util;

/**
 * Record-type prefixes used by {@link ReferenceNumberGenerator}.
 *
 * <p>The enum constant name is the literal prefix segment of the generated reference number, so
 * {@code VEN} identifies vendor codes, {@code PR} purchase request numbers, {@code RFQ} RFQ
 * numbers, {@code PO} purchase order numbers and {@code DEL} delivery numbers.
 */
public enum ReferencePrefix {
    /** Vendor codes. */
    VEN,
    /** Purchase request numbers. */
    PR,
    /** RFQ numbers. */
    RFQ,
    /** Quotation numbers. */
    QUOT,
    /** Purchase order numbers. */
    PO,
    /** Delivery numbers. */
    DEL
}
