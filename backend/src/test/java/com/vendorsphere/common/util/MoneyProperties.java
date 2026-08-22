package com.vendorsphere.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.vendorsphere.testsupport.MoneyArbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based checks for {@link Money}, the single owner of monetary and quantity scale
 * and rounding.
 *
 * <p>Extends {@link MoneyArbitraries} so the shared named providers are resolvable by
 * {@code @ForAll("...")}, and adds one local provider for lists of values whose scale is not yet
 * normalized, which is what makes the "round once at the end" clause of the property observable.
 */
class MoneyProperties extends MoneyArbitraries {

    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");
    private static final BigDecimal ZERO_QUANTITY = new BigDecimal("0.000");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    // Feature: procurement-lifecycle, Property 1: Money and quantity normalization
    @Property(tries = 200)
    void moneyAndQuantityNormalization(
            @ForAll("unscaledMoneyAmounts") BigDecimal moneyInput,
            @ForAll("unscaledQuantities") BigDecimal quantityInput,
            @ForAll("nullableMoneyAmounts") BigDecimal nullableInput,
            @ForAll("moneyAmountLists") List<BigDecimal> normalizedAmounts,
            @ForAll("unscaledMoneyAmountLists") List<BigDecimal> unscaledAmounts,
            @ForAll long permutationSeed,
            @ForAll("unclampedScores") BigDecimal unclampedScore,
            @ForAll("scores") BigDecimal inRangeScore) {

        // money(v): scale exactly 2, HALF_UP
        BigDecimal normalizedMoney = Money.money(moneyInput);
        assertThat(normalizedMoney.scale()).isEqualTo(Money.MONEY_SCALE);
        assertThat(normalizedMoney)
                .isEqualTo(moneyInput.setScale(Money.MONEY_SCALE, RoundingMode.HALF_UP));

        // quantity(v): scale exactly 3, HALF_UP
        BigDecimal normalizedQuantity = Money.quantity(quantityInput);
        assertThat(normalizedQuantity.scale()).isEqualTo(Money.QUANTITY_SCALE);
        assertThat(normalizedQuantity)
                .isEqualTo(quantityInput.setScale(Money.QUANTITY_SCALE, RoundingMode.HALF_UP));

        // null maps to 0.00 / 0.000
        assertThat(Money.money(null)).isEqualTo(ZERO_MONEY);
        assertThat(Money.money(null).scale()).isEqualTo(Money.MONEY_SCALE);
        assertThat(Money.quantity(null)).isEqualTo(ZERO_QUANTITY);
        assertThat(Money.quantity(null).scale()).isEqualTo(Money.QUANTITY_SCALE);
        if (nullableInput == null) {
            assertThat(Money.money(nullableInput)).isEqualTo(ZERO_MONEY);
            assertThat(Money.quantity(nullableInput)).isEqualTo(ZERO_QUANTITY);
        } else {
            assertThat(Money.money(nullableInput))
                    .isEqualTo(nullableInput.setScale(Money.MONEY_SCALE, RoundingMode.HALF_UP));
        }

        // sumMoney: scale-2 HALF_UP sum, invariant under permutation
        assertSumMoney(normalizedAmounts, permutationSeed);
        assertSumMoney(unscaledAmounts, permutationSeed);

        // clampScore: always inside [0.00, 100.00], identity on values already in range
        BigDecimal clamped = Money.clampScore(unclampedScore);
        assertThat(clamped.scale()).isEqualTo(Money.MONEY_SCALE);
        assertThat(clamped).isBetween(ZERO_MONEY, ONE_HUNDRED);
        assertThat(Money.clampScore(null)).isEqualTo(ZERO_MONEY);

        BigDecimal identity = Money.clampScore(inRangeScore);
        assertThat(identity.scale()).isEqualTo(Money.MONEY_SCALE);
        assertThat(identity.compareTo(inRangeScore)).isZero();
    }

    /**
     * Money.sumMoney adds exactly and rounds once at the end, so the reference value is the exact
     * sum normalized once, never a sum of per-element rounded values. Permutation invariance is
     * asserted against a shuffled copy with compareTo at scale 2.
     */
    private static void assertSumMoney(List<BigDecimal> values, long permutationSeed) {
        BigDecimal expected = values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(Money.MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal actual = Money.sumMoney(values);
        assertThat(actual.scale()).isEqualTo(Money.MONEY_SCALE);
        assertThat(actual).isEqualTo(expected);

        List<BigDecimal> shuffled = new ArrayList<>(values);
        Collections.shuffle(shuffled, new Random(permutationSeed));
        assertThat(Money.sumMoney(shuffled).compareTo(actual)).isZero();
    }

    @Provide
    Arbitrary<List<BigDecimal>> unscaledMoneyAmountLists() {
        return MoneyArbitraries.unscaledMoney().list().ofMaxSize(50);
    }
}
