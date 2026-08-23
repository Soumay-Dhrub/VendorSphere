package com.vendorsphere.vendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DocumentExpiryEvaluatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 15);

    @Test
    void absentExpiryDateIsValid() {
        assertThat(DocumentExpiryEvaluator.evaluate(null, TODAY))
                .isEqualTo(DocumentExpiryState.VALID);
    }

    @Test
    void dayBeforeTodayIsExpired() {
        assertThat(DocumentExpiryEvaluator.evaluate(TODAY.minusDays(1), TODAY))
                .isEqualTo(DocumentExpiryState.EXPIRED);
    }

    @Test
    void farPastIsExpired() {
        assertThat(DocumentExpiryEvaluator.evaluate(TODAY.minusYears(2), TODAY))
                .isEqualTo(DocumentExpiryState.EXPIRED);
    }

    @Test
    void todayIsExpiringSoon() {
        assertThat(DocumentExpiryEvaluator.evaluate(TODAY, TODAY))
                .isEqualTo(DocumentExpiryState.EXPIRING_SOON);
    }

    @Test
    void lastDayOfWindowIsExpiringSoon() {
        assertThat(DocumentExpiryEvaluator.evaluate(TODAY.plusDays(30), TODAY))
                .isEqualTo(DocumentExpiryState.EXPIRING_SOON);
    }

    @Test
    void dayAfterWindowIsValid() {
        assertThat(DocumentExpiryEvaluator.evaluate(TODAY.plusDays(31), TODAY))
                .isEqualTo(DocumentExpiryState.VALID);
    }

    @Test
    void everyDayOfTheInclusiveWindowIsExpiringSoon() {
        for (int offset = 0; offset <= DocumentExpiryEvaluator.EXPIRING_SOON_WINDOW_DAYS; offset++) {
            assertThat(DocumentExpiryEvaluator.evaluate(TODAY.plusDays(offset), TODAY))
                    .as("offset %d", offset)
                    .isEqualTo(DocumentExpiryState.EXPIRING_SOON);
        }
    }

    @Test
    void extremeExpiryDatesDoNotOverflow() {
        assertThat(DocumentExpiryEvaluator.evaluate(LocalDate.MAX, TODAY))
                .isEqualTo(DocumentExpiryState.VALID);
        assertThat(DocumentExpiryEvaluator.evaluate(LocalDate.MIN, TODAY))
                .isEqualTo(DocumentExpiryState.EXPIRED);
        assertThat(DocumentExpiryEvaluator.evaluate(LocalDate.MAX, LocalDate.MAX))
                .isEqualTo(DocumentExpiryState.EXPIRING_SOON);
    }

    @Test
    void missingEvaluationDateIsRejected() {
        assertThatNullPointerException()
                .isThrownBy(() -> DocumentExpiryEvaluator.evaluate(TODAY, null));
    }
}
