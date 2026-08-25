package com.vendorsphere.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

public final class Money {

    public static final int MONEY_SCALE = 2;
    public static final int QUANTITY_SCALE = 3;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(MONEY_SCALE);
    public static final BigDecimal ZERO_QUANTITY = BigDecimal.ZERO.setScale(QUANTITY_SCALE);

    public static final BigDecimal MIN_SCORE = ZERO_MONEY;

    public static final BigDecimal MAX_SCORE = new BigDecimal("100").setScale(MONEY_SCALE);

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private Money() {
        throw new AssertionError("No instances");
    }

    public static BigDecimal money(BigDecimal value) {
        return value == null ? ZERO_MONEY : value.setScale(MONEY_SCALE, ROUNDING);
    }

    public static BigDecimal quantity(BigDecimal value) {
        return value == null ? ZERO_QUANTITY : value.setScale(QUANTITY_SCALE, ROUNDING);
    }

    public static BigDecimal sumMoney(Collection<BigDecimal> values) {
        return money(sum(values));
    }

    public static BigDecimal sumQuantity(Collection<BigDecimal> values) {
        return quantity(sum(values));
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return ZERO_MONEY;
        }
        return money(a.multiply(b));
    }

    public static BigDecimal percentOf(BigDecimal base, BigDecimal ratePercent) {
        if (base == null || ratePercent == null) {
            return ZERO_MONEY;
        }
        return base.multiply(ratePercent).divide(ONE_HUNDRED, MONEY_SCALE, ROUNDING);
    }

    public static BigDecimal ratio(BigDecimal numerator, BigDecimal denominator, BigDecimal whenZero) {
        if (denominator == null || denominator.signum() == 0) {
            return money(whenZero);
        }
        if (numerator == null) {
            return ZERO_MONEY;
        }
        return numerator.divide(denominator, MONEY_SCALE, ROUNDING);
    }

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
