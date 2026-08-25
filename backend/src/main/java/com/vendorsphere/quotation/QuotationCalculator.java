package com.vendorsphere.quotation;

import com.vendorsphere.common.util.Money;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pure computation of every monetary figure of a quotation from vendor-supplied primitives
 * (Requirement 13). No Spring, no JPA, no clock - directly unit-testable arithmetic.
 *
 * <p>Rules, all at money scale with HALF_UP rounding:
 *
 * <ul>
 *   <li>item tax amount = quantity &times; unit price &times; tax rate / 100 (Requirement 13.1),</li>
 *   <li>item line total = quantity &times; unit price + tax amount - discount amount (Requirement
 *       13.2),</li>
 *   <li>subtotal = sum of quantity &times; unit price (Requirement 13.3),</li>
 *   <li>tax amount and discount amount are the sums of the item figures (Requirement 13.4),</li>
 *   <li>total amount = subtotal + tax amount - discount amount + shipping amount (Requirement
 *       13.5).</li>
 * </ul>
 */
public final class QuotationCalculator {

    /** One vendor-priced line: everything the vendor may supply for an item. */
    public record ItemInput(
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal taxRate,
            BigDecimal discountAmount) {
    }

    /** The computed figures of one line. */
    public record ItemTotals(BigDecimal taxAmount, BigDecimal lineTotal) {
    }

    /** The computed header figures of a quotation. */
    public record Totals(
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount) {
    }

    private QuotationCalculator() {
        throw new AssertionError("No instances");
    }

    /** Requirement 13.1 and 13.2. {@code discountAmount} defaults to zero when absent. */
    public static ItemTotals computeItem(ItemInput input) {
        BigDecimal gross = Money.multiply(input.quantity(), input.unitPrice());
        BigDecimal rate = input.taxRate() == null ? Money.ZERO_MONEY : input.taxRate();
        BigDecimal tax = Money.percentOf(gross, rate);
        BigDecimal discount = input.discountAmount() == null
                ? Money.ZERO_MONEY
                : input.discountAmount();
        return new ItemTotals(tax, Money.sumMoney(List.of(gross, tax)).subtract(discount));
    }

    /** Requirements 13.3 through 13.5 over every supplied line plus the shipping amount. */
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
