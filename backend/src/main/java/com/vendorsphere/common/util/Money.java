package com.vendorsphere.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Single owner of monetary and quantity scale and rounding.
 *
 * <p>Monetary values carry {@link #MONEY_SCALE} decimals, quantities carry
 * {@link #QUANTITY_SCALE} decimals, and every operation rounds with
 * {@link #ROUNDING}. A {@code null} input is treated as zero, so callers never
 * need null guards around arithmetic.
 */
public final class Money {

    public static final int MONEY_SCALE = 2;
    public static final int QUANTITY_SCALE = 3;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(MONEY_SCALE);
    public static final BigDecimal ZERO_QUANTITY = BigDecimal.ZERO.setScale(QUANTITY_SCALE);

    /** Lowest value {@link #clampScore(BigDecimal)} may return. */
    public static final BigDecimal MIN_SCORE = ZERO_MONEY;
    /** Highest value {@link #clampScore(BigDecimal)} may return. */
    public static final BigDecimal MAX_SCORE = new BigDecimal("100").setScale(MONEY_SCALE);

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private Money() {
        throw new AssertionError("No instances");
    }

    /** Normalizes a monetary value to scale 2, mapping {@code null} to {@code 0.00}. */
    public static BigDecimal money(BigDecimal value) {
        return value == null ? ZERO_MONEY : value.setScale(MONEY_SCALE, ROUNDING);
    }

    /** Normalizes a quantity to scale 3, mapping {@code null} to {@code 0.000}. */
    public static BigDecimal quantity(BigDecimal value) {
        return value == null ? ZERO_QUANTITY : value.setScale(QUANTITY_SCALE, ROUNDING);
    }

    /**
     * Sums monetary values at scale 2. A {@code null} collection, an empty
     * collection, and a collection of {@code null} elements all yield {@code 0.00}.
     */
    public static BigDecimal sumMoney(Collection<BigDecimal> values) {
        return money(sum(values));
    }

    /**
     * Sums quantities at scale 3. A {@code null} collection, an empty collection,
     * and a collection of {@code null} elements all yield {@code 0.000}.
     */
    public static BigDecimal sumQuantity(Collection<BigDecimal> values) {
        return quantity(sum(values));
    }

    /** Multiplies two values and rounds the product at money scale. */
    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return ZERO_MONEY;
        }
        return money(a.multiply(b));
    }

    /**
     * Applies a percentage rate to a base amount, at money scale.
     *
     * @param base        the amount the rate applies to
     * @param ratePercent the rate expressed in percent, so {@code 15.00} means 15%
     */
    public static BigDecimal percentOf(BigDecimal base, BigDecimal ratePercent) {
        if (base == null || ratePercent == null) {
            return ZERO_MONEY;
        }
        return base.multiply(ratePercent).divide(ONE_HUNDRED, MONEY_SCALE, ROUNDING);
    }

    /**
     * Divides {@code numerator} by {@code denominator} at money scale.
     *
     * @param whenZero the value returned when the denominator is {@code null} or zero,
     *                 itself normalized at money scale
     */
    public static BigDecimal ratio(BigDecimal numerator, BigDecimal denominator, BigDecimal whenZero) {
        if (denominator == null || denominator.signum() == 0) {
            return money(whenZero);
        }
        if (numerator == null) {
            return ZERO_MONEY;
        }
        return numerator.divide(denominator, MONEY_SCALE, ROUNDING);
    }

    /** Normalizes a score at money scale and clamps it into {@code [0.00, 100.00]}. */
    public static BigDecimal clampScore(BigDecimal value) {
        BigDecimal scaled = money(value);
        if (scaled.compareTo(MIN_SCORE) < 0) {
            return MIN_SCORE;
        }
        if (scaled.compareTo(MAX_SCORE) > 0) {
            return MAX_SCORE;
        }
        return scaled;
    }

    private static BigDecimal sum(Collection<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            if (value != null) {
                total = total.add(value);
            }
        }
        return total;
    }
}
