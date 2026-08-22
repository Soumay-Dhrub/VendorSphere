package com.vendorsphere.invoice;

import com.vendorsphere.common.util.StateMachine;
import java.util.EnumSet;
import java.util.Map;

/**
 * Permitted invoice status transitions, encoding acceptance criterion 24.1 exactly.
 *
 * <p>The listed pairs are SUBMITTED&rarr;UNDER_REVIEW, UNDER_REVIEW&rarr;APPROVED,
 * UNDER_REVIEW&rarr;REJECTED, APPROVED&rarr;PARTIALLY_PAID, APPROVED&rarr;PAID,
 * PARTIALLY_PAID&rarr;PAID, SUBMITTED&rarr;OVERDUE, UNDER_REVIEW&rarr;OVERDUE,
 * APPROVED&rarr;OVERDUE, PARTIALLY_PAID&rarr;OVERDUE, OVERDUE&rarr;PARTIALLY_PAID and
 * OVERDUE&rarr;PAID. No other pair is permitted; REJECTED and PAID are terminal.
 */
public final class InvoiceStatusTransitions {

    /** Immutable machine over the acceptance criterion 24.1 transition table. */
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
