package com.vendorsphere.invoice;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure three-way matching (Requirement 23): purchase order vs goods received vs invoice.
 *
 * <p>Given the per-item comparisons and the order-level facts, the engine produces the finding list
 * in precedence order. The service owns persistence, dedupe lookups and status assignment; this class
 * owns only the comparison rules:
 *
 * <ul>
 *   <li>invoiced quantity beyond cumulative received → QUANTITY_MISMATCH (Requirement 23.2),</li>
 *   <li>unit price off by more than 0.01 from the PO price → PRICE_MISMATCH (Requirement 23.3),</li>
 *   <li>no delivery recorded on the order at all → MISSING_DELIVERY (Requirement 23.4),</li>
 *   <li>an already-MATCHED invoice with identical lines → DUPLICATE_INVOICE (Requirement 23.5).</li>
 * </ul>
 */
public final class ThreeWayMatcher {

    /** Tolerance for unit price equality, pinned by Requirement 23.3. */
    public static final BigDecimal PRICE_TOLERANCE = new BigDecimal("0.01");

    /** One line-level comparison the engine receives. */
    public record ItemComparison(
            String itemName,
            BigDecimal orderedQuantity,
            BigDecimal receivedQuantity,
            BigDecimal invoicedQuantity,
            BigDecimal poUnitPrice,
            BigDecimal invoicedUnitPrice) {
    }

    /** Inputs describing an invoice against its purchase order. */
    public record MatchInput(
            String purchaseOrderNumber,
            boolean orderHasDeliveries,
            boolean duplicateOfMatchedInvoice,
            String duplicateInvoiceNumber,
            List<ItemComparison> items) {
    }

    /** One produced finding; persistence mapping belongs to the service. */
    public record Finding(
            MatchFindingType type,
            String itemName,
            String expectedValue,
            String actualValue,
            String detail) {
    }

    private ThreeWayMatcher() {
        throw new AssertionError("No instances");
    }

    /**
     * The findings for one invoice, ordered by the Requirement 23.7 precedence: DUPLICATE_INVOICE,
     * MISSING_DELIVERY, QUANTITY_MISMATCH, PRICE_MISMATCH.
     */
    public static List<Finding> match(MatchInput input) {
        List<Finding> findings = new ArrayList<>();

        if (input.duplicateOfMatchedInvoice()) {
            findings.add(new Finding(MatchFindingType.DUPLICATE_INVOICE,
                    null, input.duplicateInvoiceNumber(), null,
                    "Matches a previously MATCHED invoice of the same vendor."));
        }
        if (!input.orderHasDeliveries()) {
            findings.add(new Finding(MatchFindingType.MISSING_DELIVERY,
                    null, input.purchaseOrderNumber(), null,
                    "No goods receipt exists for this purchase order."));
        }

        for (ItemComparison item : input.items()) {
            if (item.invoicedQuantity() != null && item.receivedQuantity() != null
                    && item.invoicedQuantity().compareTo(item.receivedQuantity()) > 0) {
                findings.add(new Finding(MatchFindingType.QUANTITY_MISMATCH,
                        item.itemName(),
                        item.receivedQuantity().toPlainString(),
                        item.invoicedQuantity().toPlainString(),
                        "Invoiced quantity exceeds the cumulative received quantity."));
            }
            if (item.invoicedUnitPrice() != null && item.poUnitPrice() != null
                    && item.invoicedUnitPrice().subtract(item.poUnitPrice()).abs()
                            .compareTo(PRICE_TOLERANCE) > 0) {
                findings.add(new Finding(MatchFindingType.PRICE_MISMATCH,
                        item.itemName(),
                        item.poUnitPrice().toPlainString(),
                        item.invoicedUnitPrice().toPlainString(),
                        "Invoiced unit price differs from the purchase order price."));
            }
        }

        return orderByPrecedence(findings);
    }

    /** The highest-precedence type of a non-empty finding list (Requirement 23.7). */
    public static MatchFindingType matchStatus(List<Finding> findings) {
        return orderByPrecedence(findings).get(0).type();
    }

    private static List<Finding> orderByPrecedence(List<Finding> findings) {
        List<Finding> ordered = new ArrayList<>(findings);
        ordered.sort((a, b) -> Integer.compare(precedence(a.type()), precedence(b.type())));
        return ordered;
    }

    private static int precedence(MatchFindingType type) {
        return switch (type) {
            case DUPLICATE_INVOICE -> 0;
            case MISSING_DELIVERY -> 1;
            case QUANTITY_MISMATCH -> 2;
            case PRICE_MISMATCH -> 3;
        };
    }
}
