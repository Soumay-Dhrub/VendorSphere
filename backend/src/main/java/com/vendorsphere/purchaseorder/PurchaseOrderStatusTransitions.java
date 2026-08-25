package com.vendorsphere.purchaseorder;

import com.vendorsphere.common.util.StateMachine;
import java.util.EnumSet;
import java.util.Map;

public final class PurchaseOrderStatusTransitions {

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
