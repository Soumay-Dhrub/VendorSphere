package com.vendorsphere.rfq;

/**
 * Status of a single vendor invitation to an RFQ.
 *
 * <p>Persisted as {@code VARCHAR} through {@code @Enumerated(EnumType.STRING)}, matching the
 * {@code chk_rfq_vendor_status} constraint on {@code rfq_vendors.status}.
 */
public enum RfqVendorStatus {

    /** The vendor has been invited but has not yet opened the RFQ. */
    INVITED,

    /** A vendor user linked to the vendor has read the RFQ. */
    VIEWED,

    /** The vendor has submitted a quotation. */
    RESPONDED,

    /** The vendor has declined to quote. */
    DECLINED,

    /** The vendor's quotation was selected. */
    AWARDED
}
