package com.vendorsphere.fulfilment;

import com.vendorsphere.common.util.Money;
import com.vendorsphere.invoice.MatchFindingType;
import com.vendorsphere.invoice.ThreeWayMatcher;
import com.vendorsphere.analytics.engine.PerformanceCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FulfilmentEnginesTest {

    private static BigDecimal money(String v) {
        return new BigDecimal(v);
    }

    // ----- ThreeWayMatcher (Requirement 23) -----

    @Test
    void quantityAndPriceMismatchesAreDetected() {
        List<ThreeWayMatcher.Finding> findings = ThreeWayMatcher.match(
                new ThreeWayMatcher.MatchInput("PO-2026-001", true, false, null,
                        List.of(new ThreeWayMatcher.ItemComparison(
                                "Laptop", money("20"), money("20"), money("25"),
                                money("60000.00"), money("61000.00")))));

        assertThat(findings).extracting(ThreeWayMatcher.Finding::type)
                .containsExactly(MatchFindingType.QUANTITY_MISMATCH, MatchFindingType.PRICE_MISMATCH);
        assertThat(findings.get(0).itemName()).isEqualTo("Laptop");
        // Expected is what should be; actual is what arrived on the invoice.
        assertThat(findings.get(0).expectedValue()).isEqualTo("20");
        assertThat(findings.get(0).actualValue()).isEqualTo("25");
    }

    @Test
    void precedenceOrdersDuplicateBeforeMissingDelivery() {
        List<ThreeWayMatcher.Finding> findings = ThreeWayMatcher.match(
                new ThreeWayMatcher.MatchInput("PO-2026-001", false, true,
                        "INV-2026-0001", List.of()));

        assertThat(findings).extracting(ThreeWayMatcher.Finding::type)
                .containsExactly(MatchFindingType.DUPLICATE_INVOICE,
                        MatchFindingType.MISSING_DELIVERY);
        assertThat(ThreeWayMatcher.matchStatus(findings))
                .isEqualTo(MatchFindingType.DUPLICATE_INVOICE);
    }

    @Test
    void aOnePennyPriceDifferenceIsTolerated() {
        List<ThreeWayMatcher.Finding> findings = ThreeWayMatcher.match(
                new ThreeWayMatcher.MatchInput("PO-2026-001", true, false, null,
                        List.of(new ThreeWayMatcher.ItemComparison(
                                "Laptop", money("20"), money("20"), money("20"),
                                money("60000.00"), money("60000.01")))));

        assertThat(findings).isEmpty();
        assertThat(ThreeWayMatcher.matchStatus(List.of(
                new ThreeWayMatcher.Finding(MatchFindingType.PRICE_MISMATCH,
                        null, null, null, "x"))))
                .isEqualTo(MatchFindingType.PRICE_MISMATCH);
    }

    // ----- PerformanceCalculator (Requirement 26) -----

    @Test
    void metricsAreComputedWithDefaultsAndBoundedMeans() {
        PerformanceCalculator.Scores scores = PerformanceCalculator.compute(
                new PerformanceCalculator.Inputs(
                        10, 9,                      // delivery 90
                        money("100"), money("2"),   // quality = 100 - 2% = 98
                        new BigDecimal("0.80"),     // pricing = 80
                        0, 0,                       // responsiveness default 50
                        0, 0));                     // fulfilment default 50

        assertThat(scores.delivery()).isEqualByComparingTo(money("90.00"));
        assertThat(scores.quality()).isEqualByComparingTo(money("98.00"));
        assertThat(scores.pricing()).isEqualByComparingTo(money("80.00"));
        assertThat(scores.responsiveness()).isEqualByComparingTo(money("50.00"));
        assertThat(scores.fulfilment()).isEqualByComparingTo(money("50.00"));
        assertThat(scores.overall())
                .isEqualByComparingTo(money("73.60")); // mean of 90+98+80+50+50
    }

    @Test
    void vendorRatingDerivesFromTheScore() {
        assertThat(PerformanceCalculator.vendorRating(new BigDecimal("87.00")))
                .isEqualByComparingTo(money("4.35"));
        assertThat(PerformanceCalculator.vendorRating(Money.MAX_SCORE))
                .isEqualByComparingTo(money("5.00"));
    }
}
