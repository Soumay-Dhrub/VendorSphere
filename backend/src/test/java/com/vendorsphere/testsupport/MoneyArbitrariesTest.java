package com.vendorsphere.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

import net.jqwik.api.Arbitrary;
import org.junit.jupiter.api.Test;

class MoneyArbitrariesTest {

    private static final int SAMPLES = 200;

    @Test
    void moneyGeneratorsStayInsideDecimal15x2() {
        assertSamples(MoneyArbitraries.money(), value -> {
            assertThat(value.scale()).isEqualTo(MoneyArbitraries.MONEY_SCALE);
            assertThat(value).isBetween(BigDecimal.ZERO, MoneyArbitraries.MONEY_LINE_MAX);
        });
        assertSamples(MoneyArbitraries.positiveMoney(),
                value -> assertThat(value).isGreaterThan(BigDecimal.ZERO)
                        .isLessThanOrEqualTo(MoneyArbitraries.MONEY_LINE_MAX));
        assertSamples(MoneyArbitraries.signedMoney(),
                value -> assertThat(value.abs()).isLessThanOrEqualTo(MoneyArbitraries.MONEY_LINE_MAX));
        assertSamples(MoneyArbitraries.moneyAtColumnLimit(),
                value -> assertThat(value).isBetween(BigDecimal.ZERO, MoneyArbitraries.MONEY_MAX));
        assertSamples(MoneyArbitraries.unscaledMoney(), value -> {
            assertThat(value.scale()).isBetween(0, 6);
            assertThat(value.abs()).isLessThanOrEqualTo(MoneyArbitraries.MONEY_LINE_MAX);
        });
    }

    @Test
    void quantityGeneratorsStayInsideDecimal12x3() {
        assertSamples(MoneyArbitraries.quantity(), value -> {
            assertThat(value.scale()).isEqualTo(MoneyArbitraries.QUANTITY_SCALE);
            assertThat(value).isBetween(BigDecimal.ZERO, MoneyArbitraries.QUANTITY_LINE_MAX);
        });
        assertSamples(MoneyArbitraries.positiveQuantity(),
                value -> assertThat(value).isGreaterThan(BigDecimal.ZERO)
                        .isLessThanOrEqualTo(MoneyArbitraries.QUANTITY_LINE_MAX));
        assertSamples(MoneyArbitraries.quantityAtColumnLimit(),
                value -> assertThat(value).isBetween(BigDecimal.ZERO, MoneyArbitraries.QUANTITY_MAX));
        assertSamples(MoneyArbitraries.unscaledQuantity(),
                value -> assertThat(value.scale()).isBetween(0, 7));
    }

    @Test
    void rateAndScoreGeneratorsStayBetweenZeroAndOneHundred() {
        assertSamples(MoneyArbitraries.rate(), value -> {
            assertThat(value.scale()).isEqualTo(MoneyArbitraries.MONEY_SCALE);
            assertThat(value).isBetween(MoneyArbitraries.RATE_MIN, MoneyArbitraries.RATE_MAX);
        });
        assertSamples(MoneyArbitraries.score(),
                value -> assertThat(value).isBetween(MoneyArbitraries.RATE_MIN, MoneyArbitraries.RATE_MAX));
        assertSamples(MoneyArbitraries.unclampedScore(),
                value -> assertThat(value).isBetween(new BigDecimal("-100.00"), new BigDecimal("200.00")));
    }

    @Test
    void nullableGeneratorsProduceBothNullAndValues() {
        List<BigDecimal> samples = MoneyArbitraries.nullableMoney()
                .sampleStream().limit(500).toList();
        assertThat(samples).containsNull();
        assertThat(samples.stream().anyMatch(java.util.Objects::nonNull)).isTrue();
    }

    @Test
    void listGeneratorsStayWithinTheAggregateBudget() {
        BigDecimal budget = MoneyArbitraries.MONEY_LINE_MAX
                .multiply(BigDecimal.valueOf(50));
        assertSamples(MoneyArbitraries.moneyLists(), values -> {
            assertThat(values).hasSizeLessThanOrEqualTo(50);
            assertThat(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add))
                    .isLessThanOrEqualTo(budget);
        });
        assertSamples(MoneyArbitraries.quantityLists(),
                values -> assertThat(values).hasSizeLessThanOrEqualTo(50));
    }

    private static <T> void assertSamples(Arbitrary<T> arbitrary, Consumer<T> assertion) {
        arbitrary.sampleStream().limit(SAMPLES).forEach(assertion);
    }
}
