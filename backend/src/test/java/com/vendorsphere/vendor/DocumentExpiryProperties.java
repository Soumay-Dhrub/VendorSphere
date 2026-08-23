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

/**
 * Property-based check for {@link DocumentExpiryEvaluator}, the classification every vendor document
 * listing, the expiring-document count and the daily expiry job read their state from.
 *
 * <p>The expected state is derived from the requirement text alone — three predicates spelled out
 * independently of the evaluator — and the property asserts exactly one of them holds and that the
 * returned state is the one that did. That is what makes the classification <em>total</em> (some
 * predicate always holds) and <em>exclusive</em> (never more than one).
 *
 * <p>The predicates are expressed over epoch days rather than {@code today.plusDays(30)} so that
 * {@link LocalDate#MIN} and {@link LocalDate#MAX} are legal inputs: the generators deliberately
 * reach both ends of the supported date range, where naive date arithmetic would overflow.
 *
 * <p>A uniformly random pair of dates would land inside the 30-day window essentially never, so the
 * generator mostly produces an expiry date offset from {@code today} by ±40 days. The coverage check
 * keeps the property honest — it fails if any of the three states stops being exercised.
 */
class DocumentExpiryProperties {

    /** Inclusive window length from Requirement 5.4, stated here rather than read from the evaluator. */
    private static final int WINDOW_DAYS = 30;

    /** Offset range around {@code today}, wide enough to straddle both window boundaries. */
    private static final int NEAR_BOUNDARY_DAYS = 40;

    private static final long MIN_EPOCH_DAY = LocalDate.MIN.toEpochDay();
    private static final long MAX_EPOCH_DAY = LocalDate.MAX.toEpochDay();

    /** An evaluation date, which is never absent, and an expiry date, which may be. */
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

    /** Base pairs, with the expiry date absent one time in ten. */
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

    /**
     * Expiry dates offset from {@code today} by ±40 days, so EXPIRED, EXPIRING_SOON and VALID all
     * occur often and both window boundaries are hit. {@code today} is kept clear of the ends of the
     * range by the offset width so the shifted date stays representable.
     */
    private Arbitrary<Inputs> nearBoundaryPairs() {
        Arbitrary<LocalDate> todays = epochDays(
                        MIN_EPOCH_DAY + NEAR_BOUNDARY_DAYS, MAX_EPOCH_DAY - NEAR_BOUNDARY_DAYS)
                .map(LocalDate::ofEpochDay);
        Arbitrary<Integer> offsets =
                Arbitraries.integers().between(-NEAR_BOUNDARY_DAYS, NEAR_BOUNDARY_DAYS);
        return Combinators.combine(todays, offsets)
                .as((today, offset) -> new Inputs(today.plusDays(offset), today));
    }

    /** Independent dates spanning the whole supported range, which mostly land far from the window. */
    private Arbitrary<Inputs> unrelatedPairs() {
        Arbitrary<LocalDate> anyDate = epochDays(MIN_EPOCH_DAY, MAX_EPOCH_DAY).map(LocalDate::ofEpochDay);
        return Combinators.combine(anyDate, anyDate).as(Inputs::new);
    }

    /** Both ends of the range and the dates one step inside each window boundary from them. */
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

    /** Epoch days constrained to a range {@link LocalDate#ofEpochDay} accepts. */
    private Arbitrary<Long> epochDays(long minEpochDay, long maxEpochDay) {
        return Arbitraries.longs().between(minEpochDay, maxEpochDay);
    }
}
