package com.vendorsphere.analytics.engine;

import com.vendorsphere.common.util.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure vendor performance scoring (Requirement 26).
 *
 * <p>Five metrics, each in [0, 100] at money scale:
 *
 * <ul>
 *   <li>delivery: on-time deliveries / all deliveries (Requirement 26.1),</li>
 *   <li>quality: 100 minus damaged+rejected / received (Requirement 26.2),</li>
 *   <li>pricing: mean of peer-mean-total / own-total across the vendor's quotations, capped at 100
 *       (Requirement 26.3),</li>
 *   <li>responsiveness: quoted-before-closing / invitations (Requirement 26.4),</li>
 *   <li>fulfilment: DELIVERED-or-CLOSED orders / non-DRAFT, non-CANCELLED orders (Requirement
 *       26.5).</li>
 * </ul>
 *
 * <p>A zero denominator defaults that metric to 50.00 (Requirement 26.6); the overall score is the
 * arithmetic mean of the five (Requirement 26.7); the vendor rating maps score/20 back to [0, 5]
 * at scale 2 HALF_UP (Requirement 26.11).
 */
public final class PerformanceCalculator {

    /** The raw counts and ratios the service aggregates from stored rows. */
    public record Inputs(
            long deliveriesTotal,
            long deliveriesOnTime,
            BigDecimal receivedQuantity,
            BigDecimal rejectedQuantity,
            BigDecimal pricingRatioMean,
            long invitationsTotal,
            long quotationsBeforeClosing,
            long purchaseOrdersCounted,
            long purchaseOrdersFulfilled) {
    }

    /** The five metrics plus their mean. */
    public record Scores(
            BigDecimal delivery,
            BigDecimal quality,
            BigDecimal pricing,
            BigDecimal responsiveness,
            BigDecimal fulfilment,
            BigDecimal overall) {
    }

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal DEFAULT_METRIC = new BigDecimal("50.00");

    private PerformanceCalculator() {
        throw new AssertionError("No instances");
    }

    public static Scores compute(Inputs input) {
        BigDecimal delivery = ratio(input.deliveriesOnTime(), input.deliveriesTotal());
        BigDecimal quality = qualityScore(input.receivedQuantity(), input.rejectedQuantity());
        BigDecimal pricing = cap100(scale2(nullableRatio(input.pricingRatioMean())
                .multiply(HUNDRED)));
        BigDecimal responsiveness =
                ratio(input.quotationsBeforeClosing(), input.invitationsTotal());
        BigDecimal fulfilment =
                ratio(input.purchaseOrdersFulfilled(), input.purchaseOrdersCounted());

        BigDecimal overall = scale2(delivery.add(quality).add(pricing)
                .add(responsiveness).add(fulfilment))
                .divide(new BigDecimal("5"), Money.MONEY_SCALE, Money.ROUNDING);
        return new Scores(clamp(delivery), clamp(quality), clamp(pricing),
                clamp(responsiveness), clamp(fulfilment), clamp(overall));
    }

    /** Requirement 26.11: rating = score / 20 at scale 2 HALF_UP, staying inside [0, 5]. */
    public static BigDecimal vendorRating(BigDecimal score) {
        return scale2(score).divide(new BigDecimal("20"), Money.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal qualityScore(BigDecimal received, BigDecimal rejected) {
        if (received == null || received.signum() <= 0) {
            return DEFAULT_METRIC;
        }
        BigDecimal bad = rejected == null ? BigDecimal.ZERO : rejected;
        return scale2(HUNDRED.subtract(
                nullable(bad).multiply(HUNDRED).divide(received, 10, Money.ROUNDING)));
    }

    private static BigDecimal ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return DEFAULT_METRIC;
        }
        return scale2(new BigDecimal(numerator * 100L)
                .divide(new BigDecimal(denominator), 10, Money.ROUNDING));
    }

    private static BigDecimal nullableRatio(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal nullable(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal cap100(BigDecimal value) {
        return value.compareTo(HUNDRED) > 0 ? HUNDRED : value;
    }

    private static BigDecimal clamp(BigDecimal value) {
        BigDecimal scaled = scale2(value);
        if (scaled.compareTo(Money.MIN_SCORE) < 0) {
            return Money.MIN_SCORE;
        }
        if (scaled.compareTo(Money.MAX_SCORE) > 0) {
            return Money.MAX_SCORE;
        }
        return scaled;
    }

    private static BigDecimal scale2(BigDecimal value) {
        return Money.money(value);
    }
}
