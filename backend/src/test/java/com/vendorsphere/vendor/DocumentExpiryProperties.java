package com.vendorsphere.vendor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;

class DocumentExpiryProperties {

    private static final int WINDOW_DAYS = 30;

    private static final int NEAR_BOUNDARY_DAYS = 40;

    private static final long MIN_EPOCH_DAY = LocalDate.MIN.toEpochDay();
    private static final long MAX_EPOCH_DAY = LocalDate.MAX.toEpochDay();

    record Inputs(LocalDate expiryDate, LocalDate today) {}

    // Feature: procurement-lifecycle, Property 14: Document expiry classification is total and
    // exclusive
    // Validates: Requirement 5.4
    @Property(tries = 500)
    void documentExpiryClassificationIsTotalAndExclusive(@ForAll("inputs") Inputs inputs) {
        LocalDate expiryDate = inputs.expiryDate();
        LocalDate today = inputs.today();

        // Requirement 5.4, restated over epoch days so LocalDate.MIN/MAX cannot overflow.
        boolean present = expiryDate != null;
        long daysUntilExpiry = present ? expiryDate.toEpochDay() - today.toEpochDay() : 0L;

        boolean expectExpired = present && daysUntilExpiry < 0;
        boolean expectExpiringSoon = present && daysUntilExpiry >= 0 && daysUntilExpiry <= WINDOW_DAYS;
        boolean expectValid = !present || daysUntilExpiry > WINDOW_DAYS;

        long matching = (expectExpired ? 1 : 0) + (expectExpiringSoon ? 1 : 0) + (expectValid ? 1 : 0);
        assertThat(matching)
                .as("exactly one state must apply to expiry %s evaluated on %s", expiryDate, today)
                .isEqualTo(1);

        DocumentExpiryState expected = expectExpired
                ? DocumentExpiryState.EXPIRED
                : expectExpiringSoon ? DocumentExpiryState.EXPIRING_SOON : DocumentExpiryState.VALID;

        Statistics.label("expiry state").collect(expected.name());
        Statistics.label("expiry state").coverage(coverage -> {
            coverage.check(DocumentExpiryState.EXPIRED.name()).percentage(p -> p > 5.0);
            coverage.check(DocumentExpiryState.EXPIRING_SOON.name()).percentage(p -> p > 5.0);
            coverage.check(DocumentExpiryState.VALID.name()).percentage(p -> p > 5.0);
        });

        DocumentExpiryState actual = DocumentExpiryEvaluator.evaluate(expiryDate, today);

        assertThat(actual)
                .as("state for expiry %s evaluated on %s", expiryDate, today)
                .isNotNull()
                .isEqualTo(expected);
    }

    @Provide
    Arbitrary<Inputs> inputs() {
        return Combinators.combine(
                        datePairs(), Arbitraries.frequency(Tuple.of(9, false), Tuple.of(1, true)))
                .as((pair, expiryAbsent) ->
                        expiryAbsent ? new Inputs(null, pair.today()) : pair);
    }

    private Arbitrary<Inputs> datePairs() {
        return Arbitraries.frequencyOf(
                Tuple.of(70, nearBoundaryPairs()),
                Tuple.of(20, unrelatedPairs()),
                Tuple.of(10, extremePairs()));
    }

    private Arbitrary<Inputs> nearBoundaryPairs() {
        Arbitrary<LocalDate> todays = epochDays(
                        MIN_EPOCH_DAY + NEAR_BOUNDARY_DAYS, MAX_EPOCH_DAY - NEAR_BOUNDARY_DAYS)
                .map(LocalDate::ofEpochDay);
        Arbitrary<Integer> offsets =
                Arbitraries.integers().between(-NEAR_BOUNDARY_DAYS, NEAR_BOUNDARY_DAYS);
        return Combinators.combine(todays, offsets)
                .as((today, offset) -> new Inputs(today.plusDays(offset), today));
    }

    private Arbitrary<Inputs> unrelatedPairs() {
        Arbitrary<LocalDate> anyDate = epochDays(MIN_EPOCH_DAY, MAX_EPOCH_DAY).map(LocalDate::ofEpochDay);
        return Combinators.combine(anyDate, anyDate).as(Inputs::new);
    }

    private Arbitrary<Inputs> extremePairs() {
        Arbitrary<LocalDate> edges = Arbitraries.of(
                LocalDate.MIN,
                LocalDate.MIN.plusDays(1),
                LocalDate.MIN.plusDays(WINDOW_DAYS),
                LocalDate.MIN.plusDays(WINDOW_DAYS + 1L),
                LocalDate.EPOCH,
                LocalDate.MAX.minusDays(WINDOW_DAYS + 1L),
                LocalDate.MAX.minusDays(WINDOW_DAYS),
                LocalDate.MAX.minusDays(1),
                LocalDate.MAX);
        return Combinators.combine(edges, edges).as(Inputs::new);
    }

    private Arbitrary<Long> epochDays(long minEpochDay, long maxEpochDay) {
        return Arbitraries.longs().between(minEpochDay, maxEpochDay);
    }
}
