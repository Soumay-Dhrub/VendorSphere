package com.vendorsphere.invoice;

/**
 * Outcome of the three-way match between a purchase order, its deliveries and an invoice.
 *
 * <p>Persisted as {@code VARCHAR} through {@code @Enumerated(EnumType.STRING)}, matching the
 * {@code chk_match_status} constraint on {@code invoices.match_status}.
 */
public enum MatchStatus {

    /** Matching has not been performed yet. */
    PENDING,

    /** Quantities and prices agree and a delivery exists. */
    MATCHED,

    /** Invoiced quantity does not agree with the received quantity. */
    QUANTITY_MISMATCH,

    /** Invoiced unit price does not agree with the purchase order unit price. */
    PRICE_MISMATCH,

    /** No delivery has been recorded against the purchase order. */
    MISSING_DELIVERY,

    /** A matched invoice already exists for the same vendor and invoice number. */
    DUPLICATE_INVOICE
}
