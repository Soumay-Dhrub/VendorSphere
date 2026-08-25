package com.vendorsphere.vendor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class DocumentExpiryEvaluator {

    public static final int EXPIRING_SOON_WINDOW_DAYS = 30;

    private DocumentExpiryEvaluator() {}

    public static DocumentExpiryState evaluate(LocalDate expiryDate, LocalDate today) {
        Objects.requireNonNull(today, "today must not be null");
        if (expiryDate == null) {
            return DocumentExpiryState.VALID;
        }
        if (expiryDate.isBefore(today)) {
            return DocumentExpiryState.EXPIRED;
        }
        // Non-negative here, and epoch-day based, so no date arithmetic overflow.
        long daysUntilExpiry = ChronoUnit.DAYS.between(today, expiryDate);
        return daysUntilExpiry <= EXPIRING_SOON_WINDOW_DAYS
                ? DocumentExpiryState.EXPIRING_SOON
                : DocumentExpiryState.VALID;
    }
}
