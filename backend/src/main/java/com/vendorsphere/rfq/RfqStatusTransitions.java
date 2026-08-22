package com.vendorsphere.rfq;

import com.vendorsphere.common.util.StateMachine;
import java.util.EnumSet;
import java.util.Map;

/**
 * Permitted RFQ status transitions, encoding acceptance criterion 11.1 exactly.
 *
 * <p>The listed pairs are DRAFT&rarr;OPEN, OPEN&rarr;CLOSED, CLOSED&rarr;EVALUATION,
 * EVALUATION&rarr;AWARDED, DRAFT&rarr;CANCELLED, OPEN&rarr;CANCELLED, CLOSED&rarr;CANCELLED and
 * EVALUATION&rarr;CANCELLED. No other pair is permitted, so an AWARDED RFQ cannot be cancelled
 * (acceptance criterion 11.8).
 */
public final class RfqStatusTransitions {

    /** Immutable machine over the acceptance criterion 11.1 transition table. */
    public static final StateMachine<RfqStatus> MACHINE =
            StateMachine.of(
                    Map.of(
                            RfqStatus.DRAFT,
                            EnumSet.of(RfqStatus.OPEN, RfqStatus.CANCELLED),
                            RfqStatus.OPEN,
                            EnumSet.of(RfqStatus.CLOSED, RfqStatus.CANCELLED),
                            RfqStatus.CLOSED,
                            EnumSet.of(RfqStatus.EVALUATION, RfqStatus.CANCELLED),
                            RfqStatus.EVALUATION,
                            EnumSet.of(RfqStatus.AWARDED, RfqStatus.CANCELLED)));

    private RfqStatusTransitions() {}
}
