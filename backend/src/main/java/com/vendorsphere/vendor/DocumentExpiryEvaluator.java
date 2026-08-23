package com.vendorsphere.vendor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Pure classification of a vendor document expiry date against an evaluation date (Requirement
 * 5.4).
 *
 * <p>The evaluation date is a parameter rather than a clock field so the rule is directly testable
 * and free of Spring, persistence and time sources.
 *
 * <p>Exactly one {@link DocumentExpiryState} is returned for every input pair:
 *
 * <ul>
 *   <li>{@link DocumentExpiryState#EXPIRED} if and only if the expiry date is before the evaluation
 *       date,
 *   <li>{@link DocumentExpiryState#EXPIRING_SOON} if and only if the expiry date falls in the
 *       inclusive window {@code [today, today + 30 days]},
 *   <li>{@link DocumentExpiryState#VALID} otherwise, which includes an absent expiry date.
 * </ul>
 */
public final class DocumentExpiryEvaluator {

    /**
     * Length in days of the inclusive {@link DocumentExpiryState#EXPIRING_SOON} window that starts
     * on the evaluation date. Single source for the expiring-document count (Requirement 2.5), the
     * listing expiry state (Requirement 5.4) and the daily expiry job (Requirement 5.5).
     */
    public static final int EXPIRING_SOON_WINDOW_DAYS = 30;

    private DocumentExpiryEvaluator() {}

    /**
     * Classifies {@code expiryDate} relative to {@code today}.
     *
     * @param expiryDate the document expiry date, or {@code null} when the document has none
     * @param today the evaluation date, never {@code null}
     * @return the derived expiry state, never {@code null}
     * @throws NullPointerException when {@code today} is {@code null}; a missing evaluation date is
     *     a programming error, not an input the caller may supply
     */
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
