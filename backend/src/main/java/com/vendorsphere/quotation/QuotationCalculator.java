package com.vendorsphere.quotation;

import com.vendorsphere.common.util.Money;

import java.math.BigDecimal;
import java.util.List;

public final class QuotationCalculator {

    public record ItemInput(
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal taxRate,
            BigDecimal discountAmount) {
    }

    public record ItemTotals(BigDecimal taxAmount, BigDecimal lineTotal) {
    }

    public record Totals(
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount) {
    }

    private QuotationCalculator() {
        throw new AssertionError("No instances");
    }

    public static ItemTotals computeItem(ItemInput input) {
        BigDecimal gross = Money.multiply(input.quantity(), input.unitPrice());
        BigDecimal rate = input.taxRate() == null ? Money.ZERO_MONEY : input.taxRate();
        BigDecimal tax = Money.percentOf(gross, rate);
        BigDecimal discount = input.discountAmount() == null
                ? Money.ZERO_MONEY
                : input.discountAmount();
        return new ItemTotals(tax, Money.sumMoney(List.of(gross, tax)).subtract(discount));
    }

    public static Totals compute(List<ItemInput> items, BigDecimal shippingAmount) {
        BigDecimal subtotal = Money.ZERO_MONEY;
        BigDecimal taxTotal = Money.ZERO_MONEY;
        BigDecimal discountTotal = Money.ZERO_MONEY;
        for (ItemInput item : items) {
            subtotal = subtotal.add(Money.multiply(item.quantity(), item.unitPrice()));
            ItemTotals totals = computeItem(item);
            taxTotal = taxTotal.add(totals.taxAmount());
            discountTotal = discountTotal.add(item.discountAmount() == null
                    ? Money.ZERO_MONEY
                    : item.discountAmount());
        }
        BigDecimal shipping = shippingAmount == null ? Money.ZERO_MONEY : shippingAmount;
        return new Totals(
                Money.money(subtotal),
                Money.money(taxTotal),
                Money.money(discountTotal),
                Money.money(subtotal.add(taxTotal).subtract(discountTotal).add(shipping)));
    }
}
