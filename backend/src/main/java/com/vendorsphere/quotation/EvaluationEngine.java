package com.vendorsphere.quotation;

import com.vendorsphere.common.util.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class EvaluationEngine {

    private static final int RATIO_SCALE = 10;

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public record Weights(BigDecimal price, BigDecimal delivery, BigDecimal performance,
                          BigDecimal warranty) {

        public static final Weights DEFAULT = new Weights(
                new BigDecimal("0.40"), new BigDecimal("0.25"),
                new BigDecimal("0.25"), new BigDecimal("0.10"));
    }

    public record Input(
            UUID quotationId,
            UUID vendorId,
            BigDecimal totalAmount,
            Integer deliveryPeriodDays,
            Integer warrantyMonths,
            BigDecimal vendorPerformanceScore) {
    }

    public record Result(
            UUID quotationId,
            BigDecimal priceScore,
            BigDecimal deliveryScore,
            BigDecimal warrantyScore,
            BigDecimal performanceScore,
            BigDecimal evaluationScore,
            boolean recommended) {
    }

    private EvaluationEngine() {
        throw new AssertionError("No instances");
    }

    public static List<Result> evaluate(List<Input> inputs, Weights weights) {
        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }
        BigDecimal minTotal = inputs.stream()
                .map(Input::totalAmount)
                .filter(total -> total != null && total.signum() > 0)
                .min(BigDecimal::compareTo)
                .orElse(null);
        Integer shortestDelivery = inputs.stream()
                .map(Input::deliveryPeriodDays)
                .filter(days -> days != null && days > 0)
                .min(Integer::compareTo)
                .orElse(null);
        Integer longestWarranty = inputs.stream()
                .map(Input::warrantyMonths)
                .filter(months -> months != null && months > 0)
                .max(Integer::compareTo)
                .orElse(null);

        List<Result> scored = inputs.stream()
                .map(input -> score(input, weights, minTotal, shortestDelivery, longestWarranty))
                .toList();

        Result recommended = pickRecommended(scored, inputs);
        return scored.stream()
                .map(result -> new Result(result.quotationId(), result.priceScore(),
                        result.deliveryScore(), result.warrantyScore(), result.performanceScore(),
                        result.evaluationScore(), result == recommended))
                .toList();
    }

    private static Result score(
            Input input, Weights weights, BigDecimal minTotal,
            Integer shortestDelivery, Integer longestWarranty) {
        BigDecimal priceScore = percentScore(minTotal, input.totalAmount());
        BigDecimal deliveryScore = percentScore(shortestDelivery, input.deliveryPeriodDays());
        BigDecimal warrantyScore = percentScore(longestWarranty, input.warrantyMonths());
        BigDecimal performanceScore = input.vendorPerformanceScore() != null
                ? Money.clampScore(input.vendorPerformanceScore())
                : new BigDecimal("50.00");

        BigDecimal evaluation = Money.money(
                priceScore.multiply(weights.price())
                        .add(deliveryScore.multiply(weights.delivery()))
                        .add(performanceScore.multiply(weights.performance()))
                        .add(warrantyScore.multiply(weights.warranty())));
        return new Result(input.quotationId(), priceScore, deliveryScore, warrantyScore,
                performanceScore, evaluation, false);
    }

    private static BigDecimal percentScore(Number best, Number value) {
        if (value == null || best == null) {
            return Money.ZERO_MONEY;
        }
        long valueNumber = value.longValue();
        if (valueNumber <= 0) {
            return Money.ZERO_MONEY;
        }
        return Money.money(new BigDecimal(best.longValue() * 100L)
                .divide(new BigDecimal(valueNumber), RATIO_SCALE, Money.ROUNDING));
    }

    private static BigDecimal percentScore(BigDecimal best, BigDecimal value) {
        if (value == null || value.signum() <= 0 || best == null || best.signum() <= 0) {
            return Money.ZERO_MONEY;
        }
        return Money.money(best.multiply(HUNDRED).divide(value, RATIO_SCALE, Money.ROUNDING));
    }

    private static Result pickRecommended(List<Result> results, List<Input> inputs) {
        Result best = null;
        for (Result result : results) {
            if (best == null) {
                best = result;
                continue;
            }
            int byScore = result.evaluationScore().compareTo(best.evaluationScore());
            if (byScore > 0) {
                best = result;
            } else if (byScore == 0) {
                BigDecimal resultTotal = totalOfInput(inputs, result.quotationId());
                BigDecimal bestTotal = totalOfInput(inputs, best.quotationId());
                if (resultTotal.compareTo(bestTotal) < 0) {
                    best = result;
                }
            }
        }
        return best;
    }

    private static BigDecimal totalOfInput(List<Input> inputs, UUID quotationId) {
        return inputs.stream()
                .filter(input -> input.quotationId().equals(quotationId))
                .findFirst()
                .map(Input::totalAmount)
                .orElse(Money.ZERO_MONEY);
    }
}
