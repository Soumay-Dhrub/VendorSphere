package com.vendorsphere.analytics.engine;

import com.vendorsphere.common.util.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PerformanceCalculator {

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
