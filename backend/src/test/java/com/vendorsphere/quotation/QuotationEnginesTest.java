package com.vendorsphere.quotation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class QuotationEnginesTest {

    // ----- QuotationCalculator (Requirement 13) -----

    @Test
    void totalsAreComputedServerSideAtMoneyScale() {
        var line1 = new QuotationCalculator.ItemInput(
                new BigDecimal("20"), new BigDecimal("50000"), new BigDecimal("18"),
                new BigDecimal("10000"));
        var line2 = new QuotationCalculator.ItemInput(
                new BigDecimal("20"), new BigDecimal("5000"), BigDecimal.ZERO, null);

        QuotationCalculator.ItemTotals totals1 = QuotationCalculator.computeItem(line1);
        assertThat(totals1.taxAmount()).isEqualByComparingTo(new BigDecimal("180000.00"));
        // 1,000,000 gross + 180,000 tax - 10,000 discount.
        assertThat(totals1.lineTotal()).isEqualByComparingTo(new BigDecimal("1170000.00"));

        QuotationCalculator.Totals totals =
                QuotationCalculator.compute(List.of(line1, line2), new BigDecimal("15000"));
        assertThat(totals.subtotal()).isEqualByComparingTo(new BigDecimal("1100000.00"));
        assertThat(totals.taxAmount()).isEqualByComparingTo(new BigDecimal("180000.00"));
        assertThat(totals.discountAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(totals.totalAmount())
                .isEqualByComparingTo(new BigDecimal("1285000.00"));
    }

    // ----- EvaluationEngine (Requirement 16) -----

    private EvaluationEngine.Input input(long total, Integer deliveryDays, Integer warrantyMonths) {
        return new EvaluationEngine.Input(UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(total), deliveryDays, warrantyMonths,
                new BigDecimal("87.00"));
    }

    @Test
    void theCheapestQuotationEarnsTheFullPriceScore() {
        EvaluationEngine.Input cheap = input(1_150_000, 15, 24);
        EvaluationEngine.Input dear = input(1_200_000, 7, 36);

        List<EvaluationEngine.Result> results = EvaluationEngine.evaluate(
                List.of(cheap, dear), EvaluationEngine.Weights.DEFAULT);

        // Input order preserved: the cheap bid scores 100, the dear one its ratio.
        assertThat(results).extracting(EvaluationEngine.Result::priceScore)
                .containsExactly(new BigDecimal("100.00"), new BigDecimal("95.83"));
    }

    @Test
    void absentDeliveryOrWarrantyScoresZero() {
        EvaluationEngine.Input noExtras = input(1_000_000, null, 0);

        List<EvaluationEngine.Result> results = EvaluationEngine.evaluate(
                List.of(noExtras), EvaluationEngine.Weights.DEFAULT);

        assertThat(results.get(0).deliveryScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(results.get(0).warrantyScore()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void aVendorWithoutHistoryDefaultsToFifty() {
        EvaluationEngine.Input unknown = new EvaluationEngine.Input(UUID.randomUUID(),
                UUID.randomUUID(), BigDecimal.valueOf(1_000_000), 10, 12, null);

        List<EvaluationEngine.Result> results = EvaluationEngine.evaluate(
                List.of(unknown), EvaluationEngine.Weights.DEFAULT);

        assertThat(results.get(0).performanceScore()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void exactlyOneQuotationIsRecommendedWithLowestTotalTieBreak() {
        // Identical scores by construction: same totals, same periods, same warranties.
        EvaluationEngine.Input a = input(1_000_000, 10, 12);
        EvaluationEngine.Input b = input(1_000_000, 10, 12);
        EvaluationEngine.Input cheaper = input(900_000, 10, 12);

        List<EvaluationEngine.Result> results = EvaluationEngine.evaluate(
                List.of(a, b, cheaper), EvaluationEngine.Weights.DEFAULT);

        assertThat(results).extracting(EvaluationEngine.Result::recommended)
                .containsExactly(false, false, true);
        // The tie between a and b is broken by their identical totals - still only one winner.
        long recommendedCount = results.stream().filter(EvaluationEngine.Result::recommended).count();
        assertThat(recommendedCount).isEqualTo(1);
    }

    @Test
    void weightsShapeTheEvaluationScore() {
        EvaluationEngine.Input priceOnly = input(1_000_000, 30, 1);
        EvaluationEngine.Weights allPrice =
                new EvaluationEngine.Weights(BigDecimal.ONE, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO);

        List<EvaluationEngine.Result> results =
                EvaluationEngine.evaluate(List.of(priceOnly), allPrice);

        // A single-quotation set is best on every axis, so its evaluation score is 100 under any
        // weights; verify the weighted composition directly on a two-element set instead.
        EvaluationEngine.Input peer = input(2_000_000, 60, 6);
        List<EvaluationEngine.Result> both = EvaluationEngine.evaluate(
                List.of(priceOnly, peer), allPrice);
        assertThat(both).extracting(EvaluationEngine.Result::evaluationScore, EvaluationEngine.Result::recommended)
                .containsExactly(tuple(new BigDecimal("100.00"), true),
                        tuple(new BigDecimal("50.00"), false));
    }
}
