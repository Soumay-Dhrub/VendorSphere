package com.vendorsphere.testsupport;

import java.math.BigDecimal;
import java.util.List;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Provide;

/**
 * Shared jqwik generators for the numeric domain of VendorSphere.
 *
 * <p>Every generator stays inside the column ranges of the data model so that shrunk
 * counterexamples remain realistic and never describe a value the database could not store:
 *
 * <ul>
 *   <li>money values fit {@code DECIMAL(15,2)} &mdash; scale 2, magnitude at most {@link #MONEY_MAX}</li>
 *   <li>quantity values fit {@code DECIMAL(12,3)} &mdash; scale 3, magnitude at most {@link #QUANTITY_MAX}</li>
 *   <li>rates and scores lie in {@code 0.00}&ndash;{@code 100.00} at scale 2</li>
 * </ul>
 *
 * <p>Two flavours are offered for money and quantities. The {@code *Line} generators use the
 * conservative {@link #MONEY_LINE_MAX} / {@link #QUANTITY_LINE_MAX} bounds and are the right
 * choice whenever generated values get summed or multiplied by a property, because aggregates of
 * them still fit the column. The full-range generators exercise the column boundary itself.
 *
 * <p>Usable two ways. Extend it, and every {@code @Provide} method below is available by name,
 * because jqwik resolves named providers up the test class hierarchy:
 *
 * <pre>{@code
 * class QuotationCalculatorProperties extends MoneyArbitraries {
 *     @Property(tries = 200)
 *     void totals(@ForAll("moneyAmounts") BigDecimal shipping) { ... }
 * }
 * }</pre>
 *
 * <p>Or call the static factories from a local {@code @Provide} method, which is the better fit
 * when a property needs a composed generator:
 *
 * <pre>{@code
 * @Provide
 * Arbitrary<QuotationItemInput> quotationItems() {
 *     return Combinators.combine(MoneyArbitraries.positiveQuantity(),
 *                                MoneyArbitraries.money(),
 *                                MoneyArbitraries.rate())
 *             .as(QuotationItemInput::new);
 * }
 * }</pre>
 */
public class MoneyArbitraries {

    /** Scale of every monetary column: {@code DECIMAL(15,2)}. */
    public static final int MONEY_SCALE = 2;

    /** Scale of every quantity column: {@code DECIMAL(12,3)}. */
    public static final int QUANTITY_SCALE = 3;

    /** Largest value {@code DECIMAL(15,2)} can hold. */
    public static final BigDecimal MONEY_MAX = new BigDecimal("9999999999999.99");

    /** Smallest value {@code DECIMAL(15,2)} can hold. */
    public static final BigDecimal MONEY_MIN = MONEY_MAX.negate();

    /** Largest value {@code DECIMAL(12,3)} can hold. */
    public static final BigDecimal QUANTITY_MAX = new BigDecimal("999999999.999");

    /** Smallest value {@code DECIMAL(12,3)} can hold. */
    public static final BigDecimal QUANTITY_MIN = QUANTITY_MAX.negate();

    /** Upper bound for rates and scores. */
    public static final BigDecimal RATE_MAX = new BigDecimal("100.00");

    /** Lower bound for rates and scores. */
    public static final BigDecimal RATE_MIN = new BigDecimal("0.00");

    /**
     * Conservative money bound. A list of up to a few hundred values at this magnitude, summed and
     * taxed, still fits {@code DECIMAL(15,2)}.
     */
    public static final BigDecimal MONEY_LINE_MAX = new BigDecimal("1000000.00");

    /**
     * Conservative quantity bound. Multiplying it by {@link #MONEY_LINE_MAX} keeps the product
     * inside {@code DECIMAL(15,2)}.
     */
    public static final BigDecimal QUANTITY_LINE_MAX = new BigDecimal("1000.000");

    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");
    private static final BigDecimal ZERO_QUANTITY = new BigDecimal("0.000");
    private static final BigDecimal SMALLEST_MONEY = new BigDecimal("0.01");
    private static final BigDecimal SMALLEST_QUANTITY = new BigDecimal("0.001");

    // ---------------------------------------------------------------- money

    /** Non-negative money at scale 2, bounded by {@link #MONEY_LINE_MAX}. Safe to aggregate. */
    public static Arbitrary<BigDecimal> money() {
        return decimals(ZERO_MONEY, MONEY_LINE_MAX, MONEY_SCALE);
    }

    /** Strictly positive money at scale 2, bounded by {@link #MONEY_LINE_MAX}. */
    public static Arbitrary<BigDecimal> positiveMoney() {
        return decimals(SMALLEST_MONEY, MONEY_LINE_MAX, MONEY_SCALE);
    }

    /** Money that may be negative, at scale 2, bounded by {@link #MONEY_LINE_MAX}. */
    public static Arbitrary<BigDecimal> signedMoney() {
        return decimals(MONEY_LINE_MAX.negate(), MONEY_LINE_MAX, MONEY_SCALE);
    }

    /** Non-negative money spanning the whole {@code DECIMAL(15,2)} range. */
    public static Arbitrary<BigDecimal> moneyAtColumnLimit() {
        return decimals(ZERO_MONEY, MONEY_MAX, MONEY_SCALE);
    }

    /**
     * Money-like values at an arbitrary scale between 0 and 6, still inside the column magnitude.
     * Feed these to normalization properties that assert rounding down to scale 2.
     */
    public static Arbitrary<BigDecimal> unscaledMoney() {
        return Arbitraries.integers().between(0, 6)
                .flatMap(scale -> decimals(MONEY_LINE_MAX.negate(), MONEY_LINE_MAX, scale));
    }

    /** {@link #money()} with roughly one value in twenty replaced by {@code null}. */
    public static Arbitrary<BigDecimal> nullableMoney() {
        return money().injectNull(0.05);
    }

    /** Lists of non-negative money values, sized 0 to 50, whose sum stays inside the column. */
    public static Arbitrary<List<BigDecimal>> moneyLists() {
        return money().list().ofMaxSize(50);
    }

    // ------------------------------------------------------------- quantity

    /** Non-negative quantity at scale 3, bounded by {@link #QUANTITY_LINE_MAX}. Safe to aggregate. */
    public static Arbitrary<BigDecimal> quantity() {
        return decimals(ZERO_QUANTITY, QUANTITY_LINE_MAX, QUANTITY_SCALE);
    }

    /** Strictly positive quantity at scale 3, bounded by {@link #QUANTITY_LINE_MAX}. */
    public static Arbitrary<BigDecimal> positiveQuantity() {
        return decimals(SMALLEST_QUANTITY, QUANTITY_LINE_MAX, QUANTITY_SCALE);
    }

    /** Non-negative quantity spanning the whole {@code DECIMAL(12,3)} range. */
    public static Arbitrary<BigDecimal> quantityAtColumnLimit() {
        return decimals(ZERO_QUANTITY, QUANTITY_MAX, QUANTITY_SCALE);
    }

    /**
     * Quantity-like values at an arbitrary scale between 0 and 7, still inside the column
     * magnitude. Feed these to normalization properties that assert rounding down to scale 3.
     */
    public static Arbitrary<BigDecimal> unscaledQuantity() {
        return Arbitraries.integers().between(0, 7)
                .flatMap(scale -> decimals(QUANTITY_LINE_MAX.negate(), QUANTITY_LINE_MAX, scale));
    }

    /** {@link #quantity()} with roughly one value in twenty replaced by {@code null}. */
    public static Arbitrary<BigDecimal> nullableQuantity() {
        return quantity().injectNull(0.05);
    }

    /** Lists of non-negative quantity values, sized 0 to 50. */
    public static Arbitrary<List<BigDecimal>> quantityLists() {
        return quantity().list().ofMaxSize(50);
    }

    // -------------------------------------------------------- rates, scores

    /** A percentage rate in {@code [0.00, 100.00]} at scale 2, for tax and discount rates. */
    public static Arbitrary<BigDecimal> rate() {
        return decimals(RATE_MIN, RATE_MAX, MONEY_SCALE);
    }

    /** A score in {@code [0.00, 100.00]} at scale 2, for performance and evaluation scores. */
    public static Arbitrary<BigDecimal> score() {
        return decimals(RATE_MIN, RATE_MAX, MONEY_SCALE);
    }

    /**
     * Score-like values that reach outside {@code [0.00, 100.00]} by up to 100 in each direction.
     * Feed these to clamping properties.
     */
    public static Arbitrary<BigDecimal> unclampedScore() {
        return decimals(new BigDecimal("-100.00"), new BigDecimal("200.00"), MONEY_SCALE);
    }

    /** {@link #score()} with roughly one value in twenty replaced by {@code null}. */
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
