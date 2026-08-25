package com.vendorsphere.testsupport;

import java.math.BigDecimal;
import java.util.List;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Provide;

public class MoneyArbitraries {

    public static final int MONEY_SCALE = 2;

    public static final int QUANTITY_SCALE = 3;

    public static final BigDecimal MONEY_MAX = new BigDecimal("9999999999999.99");

    public static final BigDecimal MONEY_MIN = MONEY_MAX.negate();

    public static final BigDecimal QUANTITY_MAX = new BigDecimal("999999999.999");

    public static final BigDecimal QUANTITY_MIN = QUANTITY_MAX.negate();

    public static final BigDecimal RATE_MAX = new BigDecimal("100.00");

    public static final BigDecimal RATE_MIN = new BigDecimal("0.00");

    public static final BigDecimal MONEY_LINE_MAX = new BigDecimal("1000000.00");

    public static final BigDecimal QUANTITY_LINE_MAX = new BigDecimal("1000.000");

    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");
    private static final BigDecimal ZERO_QUANTITY = new BigDecimal("0.000");
    private static final BigDecimal SMALLEST_MONEY = new BigDecimal("0.01");
    private static final BigDecimal SMALLEST_QUANTITY = new BigDecimal("0.001");

    // ---------------------------------------------------------------- money

    public static Arbitrary<BigDecimal> money() {
        return decimals(ZERO_MONEY, MONEY_LINE_MAX, MONEY_SCALE);
    }

    public static Arbitrary<BigDecimal> positiveMoney() {
        return decimals(SMALLEST_MONEY, MONEY_LINE_MAX, MONEY_SCALE);
    }

    public static Arbitrary<BigDecimal> signedMoney() {
        return decimals(MONEY_LINE_MAX.negate(), MONEY_LINE_MAX, MONEY_SCALE);
    }

    public static Arbitrary<BigDecimal> moneyAtColumnLimit() {
        return decimals(ZERO_MONEY, MONEY_MAX, MONEY_SCALE);
    }

    public static Arbitrary<BigDecimal> unscaledMoney() {
        return Arbitraries.integers().between(0, 6)
                .flatMap(scale -> decimals(MONEY_LINE_MAX.negate(), MONEY_LINE_MAX, scale));
    }

    public static Arbitrary<BigDecimal> nullableMoney() {
        return money().injectNull(0.05);
    }

    public static Arbitrary<List<BigDecimal>> moneyLists() {
        return money().list().ofMaxSize(50);
    }

    // ------------------------------------------------------------- quantity

    public static Arbitrary<BigDecimal> quantity() {
        return decimals(ZERO_QUANTITY, QUANTITY_LINE_MAX, QUANTITY_SCALE);
    }

    public static Arbitrary<BigDecimal> positiveQuantity() {
        return decimals(SMALLEST_QUANTITY, QUANTITY_LINE_MAX, QUANTITY_SCALE);
    }

    public static Arbitrary<BigDecimal> quantityAtColumnLimit() {
        return decimals(ZERO_QUANTITY, QUANTITY_MAX, QUANTITY_SCALE);
    }

    public static Arbitrary<BigDecimal> unscaledQuantity() {
        return Arbitraries.integers().between(0, 7)
                .flatMap(scale -> decimals(QUANTITY_LINE_MAX.negate(), QUANTITY_LINE_MAX, scale));
    }

    public static Arbitrary<BigDecimal> nullableQuantity() {
        return quantity().injectNull(0.05);
    }

    public static Arbitrary<List<BigDecimal>> quantityLists() {
        return quantity().list().ofMaxSize(50);
    }

    // -------------------------------------------------------- rates, scores

    public static Arbitrary<BigDecimal> rate() {
        return decimals(RATE_MIN, RATE_MAX, MONEY_SCALE);
    }

    public static Arbitrary<BigDecimal> score() {
        return decimals(RATE_MIN, RATE_MAX, MONEY_SCALE);
    }

    public static Arbitrary<BigDecimal> unclampedScore() {
        return decimals(new BigDecimal("-100.00"), new BigDecimal("200.00"), MONEY_SCALE);
    }

    public static Arbitrary<BigDecimal> nullableScore() {
        return score().injectNull(0.05);
    }

    // ------------------------------------------- named providers for @ForAll

    @Provide
    public Arbitrary<BigDecimal> moneyAmounts() {
        return money();
    }

    @Provide
    public Arbitrary<BigDecimal> positiveMoneyAmounts() {
        return positiveMoney();
    }

    @Provide
    public Arbitrary<BigDecimal> signedMoneyAmounts() {
        return signedMoney();
    }

    @Provide
    public Arbitrary<BigDecimal> unscaledMoneyAmounts() {
        return unscaledMoney();
    }

    @Provide
    public Arbitrary<BigDecimal> nullableMoneyAmounts() {
        return nullableMoney();
    }

    @Provide
    public Arbitrary<List<BigDecimal>> moneyAmountLists() {
        return moneyLists();
    }

    @Provide
    public Arbitrary<BigDecimal> quantities() {
        return quantity();
    }

    @Provide
    public Arbitrary<BigDecimal> positiveQuantities() {
        return positiveQuantity();
    }

    @Provide
    public Arbitrary<BigDecimal> unscaledQuantities() {
        return unscaledQuantity();
    }

    @Provide
    public Arbitrary<BigDecimal> nullableQuantities() {
        return nullableQuantity();
    }

    @Provide
    public Arbitrary<List<BigDecimal>> quantityAmountLists() {
        return quantityLists();
    }

    @Provide
    public Arbitrary<BigDecimal> rates() {
        return rate();
    }

    @Provide
    public Arbitrary<BigDecimal> scores() {
        return score();
    }

    @Provide
    public Arbitrary<BigDecimal> unclampedScores() {
        return unclampedScore();
    }

    private static Arbitrary<BigDecimal> decimals(BigDecimal min, BigDecimal max, int scale) {
        return Arbitraries.bigDecimals().between(min, max).ofScale(scale);
    }
}
