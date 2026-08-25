package com.vendorsphere.rfq;

import com.vendorsphere.common.util.StateMachine;
import java.util.EnumSet;
import java.util.Map;

public final class RfqStatusTransitions {

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
