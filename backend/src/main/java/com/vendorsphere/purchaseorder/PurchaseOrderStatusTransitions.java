package com.vendorsphere.purchaseorder;

import com.vendorsphere.common.util.StateMachine;
import java.util.EnumSet;
import java.util.Map;

/**
 * Permitted purchase order status transitions, encoding acceptance criterion 19.1 exactly.
 *
 * <p>The listed pairs are DRAFT&rarr;ISSUED, ISSUED&rarr;ACKNOWLEDGED,
 * ISSUED&rarr;PARTIALLY_DELIVERED, ACKNOWLEDGED&rarr;PARTIALLY_DELIVERED,
 * ACKNOWLEDGED&rarr;DELIVERED, PARTIALLY_DELIVERED&rarr;DELIVERED, DELIVERED&rarr;CLOSED,
 * DRAFT&rarr;CANCELLED, ISSUED&rarr;CANCELLED, ACKNOWLEDGED&rarr;CANCELLED and
 * PARTIALLY_DELIVERED&rarr;CANCELLED. No other pair is permitted.
 */
public final class PurchaseOrderStatusTransitions {

    /** Immutable machine over the acceptance criterion 19.1 transition table. */
    public static final StateMachine<PurchaseOrderStatus> MACHINE =
            StateMachine.of(
                    Map.of(
                            PurchaseOrderStatus.DRAFT,
                            EnumSet.of(PurchaseOrderStatus.ISSUED, PurchaseOrderStatus.CANCELLED),
                            PurchaseOrderStatus.ISSUED,
                            EnumSet.of(
                                    PurchaseOrderStatus.ACKNOWLEDGED,
                                    PurchaseOrderStatus.PARTIALLY_DELIVERED,
                                    PurchaseOrderStatus.CANCELLED),
                            PurchaseOrderStatus.ACKNOWLEDGED,
                            EnumSet.of(
                                    PurchaseOrderStatus.PARTIALLY_DELIVERED,
                                    PurchaseOrderStatus.DELIVERED,
                                    PurchaseOrderStatus.CANCELLED),
                            PurchaseOrderStatus.PARTIALLY_DELIVERED,
                            EnumSet.of(
                                    PurchaseOrderStatus.DELIVERED, PurchaseOrderStatus.CANCELLED),
                            PurchaseOrderStatus.DELIVERED,
                            EnumSet.of(PurchaseOrderStatus.CLOSED)));

    private PurchaseOrderStatusTransitions() {}
}
