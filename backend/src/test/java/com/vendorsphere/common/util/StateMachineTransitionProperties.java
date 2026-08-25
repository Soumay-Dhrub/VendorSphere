package com.vendorsphere.common.util;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.invoice.InvoiceStatus;
import com.vendorsphere.invoice.InvoiceStatusTransitions;
import com.vendorsphere.procurement.PurchaseRequestStatus;
import com.vendorsphere.procurement.PurchaseRequestStatusTransitions;
import com.vendorsphere.purchaseorder.PurchaseOrderStatus;
import com.vendorsphere.purchaseorder.PurchaseOrderStatusTransitions;
import com.vendorsphere.rfq.RfqStatus;
import com.vendorsphere.rfq.RfqStatusTransitions;
import com.vendorsphere.vendor.VendorStatus;
import com.vendorsphere.vendor.VendorStatusTransitions;
import java.util.Map;
import java.util.Set;
import net.jqwik.api.ForAll;
import net.jqwik.api.GenerationMode;
import net.jqwik.api.Property;
import org.springframework.http.HttpStatus;

class StateMachineTransitionProperties {

    private static final Set<Map.Entry<VendorStatus, VendorStatus>> VENDOR_EDGES =
            Set.of(
                    entry(VendorStatus.PROSPECTIVE, VendorStatus.ACTIVE),
                    entry(VendorStatus.PROSPECTIVE, VendorStatus.INACTIVE),
                    entry(VendorStatus.ACTIVE, VendorStatus.SUSPENDED),
                    entry(VendorStatus.ACTIVE, VendorStatus.BLACKLISTED),
                    entry(VendorStatus.ACTIVE, VendorStatus.INACTIVE),
                    entry(VendorStatus.SUSPENDED, VendorStatus.ACTIVE),
                    entry(VendorStatus.SUSPENDED, VendorStatus.BLACKLISTED),
                    entry(VendorStatus.SUSPENDED, VendorStatus.INACTIVE),
                    entry(VendorStatus.BLACKLISTED, VendorStatus.INACTIVE),
                    entry(VendorStatus.INACTIVE, VendorStatus.ACTIVE));

    private static final Set<Map.Entry<PurchaseRequestStatus, PurchaseRequestStatus>>
            PURCHASE_REQUEST_EDGES =
                    Set.of(
                            entry(PurchaseRequestStatus.DRAFT, PurchaseRequestStatus.SUBMITTED),
                            entry(
                                    PurchaseRequestStatus.SUBMITTED,
                                    PurchaseRequestStatus.UNDER_REVIEW),
                            entry(
                                    PurchaseRequestStatus.UNDER_REVIEW,
                                    PurchaseRequestStatus.APPROVED),
                            entry(
                                    PurchaseRequestStatus.UNDER_REVIEW,
                                    PurchaseRequestStatus.REJECTED),
                            entry(
                                    PurchaseRequestStatus.APPROVED,
                                    PurchaseRequestStatus.PROCUREMENT_STARTED),
                            entry(
                                    PurchaseRequestStatus.PROCUREMENT_STARTED,
                                    PurchaseRequestStatus.COMPLETED));

    private static final Set<Map.Entry<RfqStatus, RfqStatus>> RFQ_EDGES =
            Set.of(
                    entry(RfqStatus.DRAFT, RfqStatus.OPEN),
                    entry(RfqStatus.OPEN, RfqStatus.CLOSED),
                    entry(RfqStatus.CLOSED, RfqStatus.EVALUATION),
                    entry(RfqStatus.EVALUATION, RfqStatus.AWARDED),
                    entry(RfqStatus.DRAFT, RfqStatus.CANCELLED),
                    entry(RfqStatus.OPEN, RfqStatus.CANCELLED),
                    entry(RfqStatus.CLOSED, RfqStatus.CANCELLED),
                    entry(RfqStatus.EVALUATION, RfqStatus.CANCELLED));

    private static final Set<Map.Entry<PurchaseOrderStatus, PurchaseOrderStatus>>
            PURCHASE_ORDER_EDGES =
                    Set.of(
                            entry(PurchaseOrderStatus.DRAFT, PurchaseOrderStatus.ISSUED),
                            entry(PurchaseOrderStatus.ISSUED, PurchaseOrderStatus.ACKNOWLEDGED),
                            entry(
                                    PurchaseOrderStatus.ISSUED,
                                    PurchaseOrderStatus.PARTIALLY_DELIVERED),
                            entry(
                                    PurchaseOrderStatus.ACKNOWLEDGED,
                                    PurchaseOrderStatus.PARTIALLY_DELIVERED),
                            entry(PurchaseOrderStatus.ACKNOWLEDGED, PurchaseOrderStatus.DELIVERED),
                            entry(
                                    PurchaseOrderStatus.PARTIALLY_DELIVERED,
                                    PurchaseOrderStatus.DELIVERED),
                            entry(PurchaseOrderStatus.DELIVERED, PurchaseOrderStatus.CLOSED),
                            entry(PurchaseOrderStatus.DRAFT, PurchaseOrderStatus.CANCELLED),
                            entry(PurchaseOrderStatus.ISSUED, PurchaseOrderStatus.CANCELLED),
                            entry(PurchaseOrderStatus.ACKNOWLEDGED, PurchaseOrderStatus.CANCELLED),
                            entry(
                                    PurchaseOrderStatus.PARTIALLY_DELIVERED,
                                    PurchaseOrderStatus.CANCELLED));

    private static final Set<Map.Entry<InvoiceStatus, InvoiceStatus>> INVOICE_EDGES =
            Set.of(
                    entry(InvoiceStatus.SUBMITTED, InvoiceStatus.UNDER_REVIEW),
                    entry(InvoiceStatus.UNDER_REVIEW, InvoiceStatus.APPROVED),
                    entry(InvoiceStatus.UNDER_REVIEW, InvoiceStatus.REJECTED),
                    entry(InvoiceStatus.APPROVED, InvoiceStatus.PARTIALLY_PAID),
                    entry(InvoiceStatus.APPROVED, InvoiceStatus.PAID),
                    entry(InvoiceStatus.PARTIALLY_PAID, InvoiceStatus.PAID),
                    entry(InvoiceStatus.SUBMITTED, InvoiceStatus.OVERDUE),
                    entry(InvoiceStatus.UNDER_REVIEW, InvoiceStatus.OVERDUE),
                    entry(InvoiceStatus.APPROVED, InvoiceStatus.OVERDUE),
                    entry(InvoiceStatus.PARTIALLY_PAID, InvoiceStatus.OVERDUE),
                    entry(InvoiceStatus.OVERDUE, InvoiceStatus.PARTIALLY_PAID),
                    entry(InvoiceStatus.OVERDUE, InvoiceStatus.PAID));

    // Feature: procurement-lifecycle, Property 2: State machines accept exactly the listed
    // transitions
    // Validates: Requirements 3.1, 3.2
    @Property(generation = GenerationMode.EXHAUSTIVE)
    void vendorStatusMachineMatchesRequirement(
            @ForAll VendorStatus from, @ForAll VendorStatus to) {
        assertThat(VENDOR_EDGES).hasSize(10);
        assertAgreesWithRequirement(VendorStatusTransitions.MACHINE, VENDOR_EDGES, from, to);
    }

    // Feature: procurement-lifecycle, Property 2: State machines accept exactly the listed
    // transitions
    // Validates: Requirements 8.1, 8.2
    @Property(generation = GenerationMode.EXHAUSTIVE)
    void purchaseRequestStatusMachineMatchesRequirement(
            @ForAll PurchaseRequestStatus from, @ForAll PurchaseRequestStatus to) {
        assertThat(PURCHASE_REQUEST_EDGES).hasSize(6);
        assertAgreesWithRequirement(
                PurchaseRequestStatusTransitions.MACHINE, PURCHASE_REQUEST_EDGES, from, to);
    }

    // Feature: procurement-lifecycle, Property 2: State machines accept exactly the listed
    // transitions
    // Validates: Requirements 11.1, 11.2
    @Property(generation = GenerationMode.EXHAUSTIVE)
    void rfqStatusMachineMatchesRequirement(@ForAll RfqStatus from, @ForAll RfqStatus to) {
        assertThat(RFQ_EDGES).hasSize(8);
        assertAgreesWithRequirement(RfqStatusTransitions.MACHINE, RFQ_EDGES, from, to);
    }

    // Feature: procurement-lifecycle, Property 2: State machines accept exactly the listed
    // transitions
    // Validates: Requirements 19.1, 19.2
    @Property(generation = GenerationMode.EXHAUSTIVE)
    void purchaseOrderStatusMachineMatchesRequirement(
            @ForAll PurchaseOrderStatus from, @ForAll PurchaseOrderStatus to) {
        assertThat(PURCHASE_ORDER_EDGES).hasSize(11);
        assertAgreesWithRequirement(
                PurchaseOrderStatusTransitions.MACHINE, PURCHASE_ORDER_EDGES, from, to);
    }

    // Feature: procurement-lifecycle, Property 2: State machines accept exactly the listed
    // transitions
    // Validates: Requirements 24.1, 24.2
    @Property(generation = GenerationMode.EXHAUSTIVE)
    void invoiceStatusMachineMatchesRequirement(
            @ForAll InvoiceStatus from, @ForAll InvoiceStatus to) {
        assertThat(INVOICE_EDGES).hasSize(12);
        assertAgreesWithRequirement(InvoiceStatusTransitions.MACHINE, INVOICE_EDGES, from, to);
    }

    private static <S extends Enum<S>> void assertAgreesWithRequirement(
            StateMachine<S> machine, Set<Map.Entry<S, S>> listedEdges, S from, S to) {

        boolean listed = listedEdges.contains(entry(from, to));

        assertThat(machine.permits(from, to))
                .as("permits(%s, %s) must be %s", from, to, listed)
                .isEqualTo(listed);

        if (listed) {
            assertThatCode(() -> machine.assertTransition(from, to))
                    .as("assertTransition(%s, %s) must not throw", from, to)
                    .doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> machine.assertTransition(from, to))
                    .as("assertTransition(%s, %s) must be refused", from, to)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(
                            thrown -> {
                                BusinessException refusal = (BusinessException) thrown;
                                assertThat(refusal.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                                assertThat(refusal.getMessage())
                                        .contains(from.name())
                                        .contains(to.name());
                            });
        }
    }
}
