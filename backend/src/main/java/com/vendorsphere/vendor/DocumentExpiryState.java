package com.vendorsphere.vendor;

/**
 * Derived expiry state of a vendor document, computed against the request date (Requirement 5.4).
 *
 * <p>This state is not stored; it is derived from the document expiry date on read.
 */
public enum DocumentExpiryState {

    /** No expiry date, or the expiry date is more than 30 days after the request date. */
    VALID,

    /** The expiry date falls between the request date and 30 days after it, inclusive. */
    EXPIRING_SOON,

    /** The expiry date is before the request date. */
    EXPIRED
}
