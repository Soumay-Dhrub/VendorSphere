package com.vendorsphere.procurement;

import com.vendorsphere.common.util.StateMachine;
import java.util.EnumSet;
import java.util.Map;

/**
 * Permitted purchase request status transitions, encoding acceptance criterion 8.1 exactly.
 *
 * <p>The listed pairs are DRAFT&rarr;SUBMITTED, SUBMITTED&rarr;UNDER_REVIEW,
 * UNDER_REVIEW&rarr;APPROVED, UNDER_REVIEW&rarr;REJECTED, APPROVED&rarr;PROCUREMENT_STARTED and
 * PROCUREMENT_STARTED&rarr;COMPLETED. No other pair is permitted; REJECTED and COMPLETED are
 * terminal.
 */
public final class PurchaseRequestStatusTransitions {

    /** Immutable machine over the acceptance criterion 8.1 transition table. */
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
