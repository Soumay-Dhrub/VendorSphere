package com.vendorsphere.common.attachment;

/**
 * Record type that owns an attachment. Together with the owner identifier this discriminates the
 * rows of the polymorphic {@code attachments} table.
 *
 * <p>Persisted as {@code VARCHAR} through {@code @Enumerated(EnumType.STRING)}.
 */
public enum AttachmentOwnerType {

    /** A vendor compliance document. */
    VENDOR_DOCUMENT,

    /** A supporting file on a purchase request. */
    PURCHASE_REQUEST,

    /** A specification or terms document on an RFQ. */
    RFQ,

    /** A supporting document on a vendor quotation. */
    QUOTATION,

    /** Proof of delivery for a goods receipt. */
    DELIVERY_PROOF,

    /** The invoice document itself. */
    INVOICE
}
