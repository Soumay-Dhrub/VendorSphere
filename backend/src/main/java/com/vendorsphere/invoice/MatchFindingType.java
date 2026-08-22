package com.vendorsphere.invoice;

/**
 * Type of a single three-way matching discrepancy.
 *
 * <p>The declaration order of this enum is the precedence order of Requirement 23.7: when several
 * finding types apply to one invoice, the earliest declared type determines the resulting
 * {@link MatchStatus}. Callers may therefore rely on {@link Enum#compareTo} and on the natural
 * ordering of {@code EnumSet} for precedence. Do not reorder these constants.
 *
 * <p>Persisted as {@code VARCHAR} through {@code @Enumerated(EnumType.STRING)}.
 */
public enum MatchFindingType {

    /** A matched invoice already exists for the same vendor and invoice number. */
    DUPLICATE_INVOICE,

    /** No delivery has been recorded against the purchase order. */
    MISSING_DELIVERY,

    /** Invoiced quantity does not agree with the received quantity. */
    QUANTITY_MISMATCH,

    /** Invoiced unit price does not agree with the purchase order unit price. */
    PRICE_MISMATCH
}
