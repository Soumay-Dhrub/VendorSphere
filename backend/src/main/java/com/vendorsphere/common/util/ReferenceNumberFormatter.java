package com.vendorsphere.common.util;

import java.util.Objects;

public final class ReferenceNumberFormatter {

    public static final int MIN_SEQUENCE_DIGITS = 3;

    public static final int MIN_SEQUENCE = 1;

    public static final int MIN_YEAR = 0;

    public static final int MAX_YEAR = 9999;

    private static final String PATTERN = "%s-%04d-%0" + MIN_SEQUENCE_DIGITS + "d";

    private ReferenceNumberFormatter() {
        throw new AssertionError("No instances");
    }

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
