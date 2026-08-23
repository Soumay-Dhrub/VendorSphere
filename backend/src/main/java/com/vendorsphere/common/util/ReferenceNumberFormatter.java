package com.vendorsphere.common.util;

import java.util.Objects;

/**
 * Pure formatter for business reference numbers.
 *
 * <p>Produces {@code {PREFIX}-{YYYY}-{NNN}} where {@code PREFIX} is the {@link ReferencePrefix}
 * name, {@code YYYY} is the four-digit allocation year and {@code NNN} is the allocated sequence
 * value zero-padded to at least {@link #MIN_SEQUENCE_DIGITS} digits. Sequence values above 999
 * simply grow, so {@code 1000} formats as {@code 1000} rather than being truncated.
 *
 * <p>This class holds no state and touches neither the database nor Spring, so the format rules of
 * Requirements 1.1, 1.2 and 1.4 can be exercised directly.
 */
public final class ReferenceNumberFormatter {

    /** Minimum width of the sequence segment; smaller values are zero-padded to this width. */
    public static final int MIN_SEQUENCE_DIGITS = 3;

    /** Lowest sequence value the sequence table can hand out. */
    public static final int MIN_SEQUENCE = 1;

    /** Lowest year that still renders as four digits once zero-padded. */
    public static final int MIN_YEAR = 0;

    /** Highest year that renders as four digits. */
    public static final int MAX_YEAR = 9999;

    private static final String PATTERN = "%s-%04d-%0" + MIN_SEQUENCE_DIGITS + "d";

    private ReferenceNumberFormatter() {
        throw new AssertionError("No instances");
    }

    /**
     * Formats one allocated sequence value as a reference number.
     *
     * @param prefix the record type prefix, never {@code null}
     * @param year the four-digit calendar year of allocation, in {@code [0, 9999]}
     * @param sequence the allocated sequence value, at least {@code 1}
     * @return the reference number, for example {@code RFQ-2026-001}
     * @throws NullPointerException when {@code prefix} is {@code null}
     * @throws IllegalArgumentException when {@code year} falls outside {@code [0, 9999]} or
     *     {@code sequence} is below {@code 1}
     */
    public static String format(ReferencePrefix prefix, int year, int sequence) {
        Objects.requireNonNull(prefix, "prefix must not be null");
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new IllegalArgumentException(
                    "year must be between " + MIN_YEAR + " and " + MAX_YEAR + " but was " + year);
        }
        if (sequence < MIN_SEQUENCE) {
            throw new IllegalArgumentException(
                    "sequence must be at least " + MIN_SEQUENCE + " but was " + sequence);
        }
        return String.format(PATTERN, prefix.name(), year, sequence);
    }
}
