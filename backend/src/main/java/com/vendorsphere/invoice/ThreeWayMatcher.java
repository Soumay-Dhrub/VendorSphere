package com.vendorsphere.invoice;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ThreeWayMatcher {

    public static final BigDecimal PRICE_TOLERANCE = new BigDecimal("0.01");

    public record ItemComparison(
            String itemName,
            BigDecimal orderedQuantity,
            BigDecimal receivedQuantity,
            BigDecimal invoicedQuantity,
            BigDecimal poUnitPrice,
            BigDecimal invoicedUnitPrice) {
    }

    public record MatchInput(
            String purchaseOrderNumber,
            boolean orderHasDeliveries,
            boolean duplicateOfMatchedInvoice,
            String duplicateInvoiceNumber,
            List<ItemComparison> items) {
    }

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
