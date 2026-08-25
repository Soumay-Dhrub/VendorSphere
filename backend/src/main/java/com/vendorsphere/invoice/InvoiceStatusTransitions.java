package com.vendorsphere.invoice;

import com.vendorsphere.common.util.StateMachine;
import java.util.EnumSet;
import java.util.Map;

public final class InvoiceStatusTransitions {

    public static final StateMachine<InvoiceStatus> MACHINE =
            StateMachine.of(
                    Map.of(
                            InvoiceStatus.SUBMITTED,
                            EnumSet.of(InvoiceStatus.UNDER_REVIEW, InvoiceStatus.OVERDUE),
                            InvoiceStatus.UNDER_REVIEW,
                            EnumSet.of(
                                    InvoiceStatus.APPROVED,
                                    InvoiceStatus.REJECTED,
                                    InvoiceStatus.OVERDUE),
                            InvoiceStatus.APPROVED,
                            EnumSet.of(
                                    InvoiceStatus.PARTIALLY_PAID,
                                    InvoiceStatus.PAID,
                                    InvoiceStatus.OVERDUE),
                            InvoiceStatus.PARTIALLY_PAID,
                            EnumSet.of(InvoiceStatus.PAID, InvoiceStatus.OVERDUE),
                            InvoiceStatus.OVERDUE,
                            EnumSet.of(InvoiceStatus.PARTIALLY_PAID, InvoiceStatus.PAID)));

    private InvoiceStatusTransitions() {}
}
