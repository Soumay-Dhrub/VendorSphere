package com.vendorsphere.procurement;

import com.vendorsphere.common.util.StateMachine;
import java.util.EnumSet;
import java.util.Map;

public final class PurchaseRequestStatusTransitions {

    public static final StateMachine<PurchaseRequestStatus> MACHINE =
            StateMachine.of(
                    Map.of(
                            PurchaseRequestStatus.DRAFT,
                            EnumSet.of(PurchaseRequestStatus.SUBMITTED),
                            PurchaseRequestStatus.SUBMITTED,
                            EnumSet.of(PurchaseRequestStatus.UNDER_REVIEW),
                            PurchaseRequestStatus.UNDER_REVIEW,
                            EnumSet.of(
                                    PurchaseRequestStatus.APPROVED, PurchaseRequestStatus.REJECTED),
                            PurchaseRequestStatus.APPROVED,
                            EnumSet.of(PurchaseRequestStatus.PROCUREMENT_STARTED),
                            PurchaseRequestStatus.PROCUREMENT_STARTED,
                            EnumSet.of(PurchaseRequestStatus.COMPLETED)));

    private PurchaseRequestStatusTransitions() {}
}
