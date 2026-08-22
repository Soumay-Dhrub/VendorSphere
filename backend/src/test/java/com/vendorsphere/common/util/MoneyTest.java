package com.vendorsphere.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    @DisplayName("money normalizes to scale 2 with HALF_UP and maps null to 0.00")
    void moneyNormalizes() {
        assertThat(Money.money(null)).isEqualTo(new BigDecimal("0.00"));
        assertThat(Money.money(new BigDecimal("5"))).isEqualTo(new BigDecimal("5.00"));
        assertThat(Money.money(new BigDecimal("1.005"))).isEqualTo(new BigDecimal("1.01"));
        assertThat(Money.money(new BigDecimal("-1.005"))).isEqualTo(new BigDecimal("-1.01"));
    }

    @Test
    @DisplayName("quantity normalizes to scale 3 with HALF_UP and maps null to 0.000")
    void quantityNormalizes() {
        assertThat(Money.quantity(null)).isEqualTo(new BigDecimal("0.000"));
        assertThat(Money.quantity(new BigDecimal("2.5"))).isEqualTo(new BigDecimal("2.500"));
        assertThat(Money.quantity(new BigDecimal("2.0005"))).isEqualTo(new BigDecimal("2.001"));
    }

    @Test
    @DisplayName("sums ignore null elements and return zero for empty or null collections")
    void sumsHandleNulls() {
        assertThat(Money.sumMoney(null)).isEqualTo(new BigDecimal("0.00"));
        assertThat(Money.sumMoney(List.of())).isEqualTo(new BigDecimal("0.00"));
        assertThat(Money.sumMoney(Arrays.asList(new BigDecimal("10.005"), null, new BigDecimal("0.004"))))
                .isEqualTo(new BigDecimal("10.01"));

        assertThat(Money.sumQuantity(null)).isEqualTo(new BigDecimal("0.000"));
        assertThat(Money.sumQuantity(Arrays.asList(new BigDecimal("1.5"), null, new BigDecimal("2.25"))))
                .isEqualTo(new BigDecimal("3.750"));
    }

    @Test
    @DisplayName("multiply rounds the exact product once at money scale")
    void multiplyRoundsAtMoneyScale() {
        assertThat(Money.multiply(new BigDecimal("3.333"), new BigDecimal("3")))
                .isEqualTo(new BigDecimal("10.00"));
        assertThat(Money.multiply(null, new BigDecimal("3"))).isEqualTo(new BigDecimal("0.00"));
        assertThat(Money.multiply(new BigDecimal("3"), null)).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("percentOf applies a percent rate at money scale")
    void percentOfAppliesRate() {
        assertThat(Money.percentOf(new BigDecimal("100.00"), new BigDecimal("15.00")))
                .isEqualTo(new BigDecimal("15.00"));
        assertThat(Money.percentOf(new BigDecimal("10.10"), new BigDecimal("7.50")))
                .isEqualTo(new BigDecimal("0.76"));
        assertThat(Money.percentOf(null, new BigDecimal("7.50"))).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("ratio returns the fallback when the denominator is null or zero")
    void ratioFallsBack() {
        assertThat(Money.ratio(new BigDecimal("1.00"), BigDecimal.ZERO, new BigDecimal("1")))
                .isEqualTo(new BigDecimal("1.00"));
        assertThat(Money.ratio(new BigDecimal("1.00"), null, null)).isEqualTo(new BigDecimal("0.00"));
        assertThat(Money.ratio(new BigDecimal("1"), new BigDecimal("3"), BigDecimal.ONE))
                .isEqualTo(new BigDecimal("0.33"));
    }

    @Test
    @DisplayName("clampScore clamps into [0.00, 100.00] and leaves in-range values alone")
    void clampScoreClamps() {
        assertThat(Money.clampScore(new BigDecimal("-0.01"))).isEqualTo(new BigDecimal("0.00"));
        assertThat(Money.clampScore(new BigDecimal("100.01"))).isEqualTo(new BigDecimal("100.00"));
        assertThat(Money.clampScore(new BigDecimal("73.45"))).isEqualTo(new BigDecimal("73.45"));
        assertThat(Money.clampScore(null)).isEqualTo(new BigDecimal("0.00"));
    }
}
