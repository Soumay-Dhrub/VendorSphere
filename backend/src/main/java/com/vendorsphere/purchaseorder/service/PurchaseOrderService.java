package com.vendorsphere.purchaseorder.service;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.service.AuditService;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.common.util.Money;
import com.vendorsphere.common.util.ReferenceNumberGenerator;
import com.vendorsphere.common.util.ReferencePrefix;
import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.service.NotificationService;
import com.vendorsphere.purchaseorder.PurchaseOrderStatus;
import com.vendorsphere.purchaseorder.PurchaseOrderStatusTransitions;
import com.vendorsphere.quotation.QuotationStatus;
import com.vendorsphere.quotation.entity.QuotationItem;
import com.vendorsphere.quotation.entity.VendorSelection;
import com.vendorsphere.quotation.repository.QuotationItemRepository;
import com.vendorsphere.quotation.repository.VendorSelectionRepository;
import com.vendorsphere.rfq.entity.Rfq;
import com.vendorsphere.rfq.repository.RfqRepository;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.user.repository.UserRepository;
import com.vendorsphere.vendor.service.VendorAccessGuard;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class PurchaseOrderService {

    static final String NO_SELECTION_MESSAGE = "RFQ has no selected quotation";

    static final String CANCELLATION_REASON_MESSAGE = "Cancellation reason is required";

    static final String DELIVERIES_EXIST_MESSAGE =
            "Purchase order with recorded deliveries cannot be cancelled";

    static final String NOT_FOUND_MESSAGE = "Purchase order not found";
    static final String RFQ_NOT_FOUND_MESSAGE = "RFQ not found";

    private final RfqRepository rfqRepository;
    private final VendorSelectionRepository selectionRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final com.vendorsphere.purchaseorder.repository.PurchaseOrderRepository poRepository;
    private final com.vendorsphere.purchaseorder.repository.PurchaseOrderItemRepository
            poItemRepository;
    private final UserRepository userRepository;
    private final com.vendorsphere.organization.repository.OrganizationRepository
            organizationRepository;
    private final ReferenceNumberGenerator referenceNumberGenerator;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final VendorAccessGuard vendorAccessGuard;
    private final com.vendorsphere.analytics.service.PerformanceEngine performanceEngine;
    private final Clock clock;

    public PurchaseOrderService(
            RfqRepository rfqRepository,
            VendorSelectionRepository selectionRepository,
            QuotationItemRepository quotationItemRepository,
            com.vendorsphere.purchaseorder.repository.PurchaseOrderRepository poRepository,
            com.vendorsphere.purchaseorder.repository.PurchaseOrderItemRepository poItemRepository,
            UserRepository userRepository,
            com.vendorsphere.organization.repository.OrganizationRepository organizationRepository,
            ReferenceNumberGenerator referenceNumberGenerator,
            NotificationService notificationService,
            AuditService auditService,
            VendorAccessGuard vendorAccessGuard,
            com.vendorsphere.analytics.service.PerformanceEngine performanceEngine,
            Clock clock
    ) {
        this.rfqRepository = rfqRepository;
        this.selectionRepository = selectionRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.poRepository = poRepository;
        this.poItemRepository = poItemRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.referenceNumberGenerator = referenceNumberGenerator;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.vendorAccessGuard = vendorAccessGuard;
        this.performanceEngine = performanceEngine;
        this.clock = clock;
    }

    @Transactional
    public com.vendorsphere.purchaseorder.dto.PurchaseOrderResponse generate(UUID rfqId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Rfq rfq = rfqRepository.findByIdAndOrganizationId(rfqId, organizationId)
                .orElseThrow(() -> new BusinessException(RFQ_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));

        VendorSelection selection = selectionRepository.findByRfqId(rfq.getId())
                .orElseThrow(() -> new BusinessException(NO_SELECTION_MESSAGE, HttpStatus.CONFLICT));

        poRepository.findByRfqId(rfq.getId()).ifPresent(existing -> {
            throw new BusinessException(
                    "Purchase order " + existing.getPoNumber() + " already exists for this RFQ",
                    HttpStatus.CONFLICT);
        });

        var quotation = selection.getQuotation();
        var po = new com.vendorsphere.purchaseorder.entity.PurchaseOrder();
        po.setOrganization(organizationRepository.getReferenceById(organizationId));
        po.setRfq(rfq);
        po.setQuotation(quotation);
        po.setVendor(quotation.getVendor());
        po.setPoNumber(referenceNumberGenerator.allocate(organizationId, ReferencePrefix.PO));
        // Requirement 18.4: address from the RFQ, terms from the quotation.
        po.setDeliveryAddress(rfq.getDeliveryLocation());
        po.setPaymentTerms(quotation.getPaymentTerms());
        if (quotation.getDeliveryPeriodDays() != null) {
            LocalDate today = LocalDate.now(clock);
            po.setExpectedDelivery(today.plusDays(quotation.getDeliveryPeriodDays()));
        }
        po.setSubtotal(quotation.getSubtotal());
        po.setTaxAmount(quotation.getTaxAmount());
        po.setTotalAmount(quotation.getTotalAmount());
        po.setStatus(PurchaseOrderStatus.DRAFT);

        var saved = poRepository.save(po);

        for (QuotationItem quoted : quotationItemRepository
                .findByQuotationIdOrderByCreatedAtAscIdAsc(quotation.getId())) {
            var item = new com.vendorsphere.purchaseorder.entity.PurchaseOrderItem();
            item.setPurchaseOrder(saved);
            item.setItemName(quoted.getItemName());
            item.setQuantity(Money.quantity(quoted.getQuantity()));
            item.setUnitPrice(Money.money(quoted.getUnitPrice()));
            item.setTaxRate(quoted.getTaxRate() == null ? Money.ZERO_MONEY : quoted.getTaxRate());
            item.setTaxAmount(quoted.getTaxAmount());
            item.setLineTotal(quoted.getLineTotal());
            item.setDeliveredQuantity(Money.ZERO_QUANTITY);
            poItemRepository.save(item);
        }

        auditService.record(AuditAction.PURCHASE_ORDER_GENERATED, "PurchaseOrder", saved.getId(),
                null, saved.getPoNumber() + " from " + rfq.getRfqNumber());
        return toDetailResponse(saved);
    }

    @Transactional
    public com.vendorsphere.purchaseorder.dto.PurchaseOrderResponse issue(UUID poId) {
        var po = findInternal(poId);
        transition(po.getStatus(), PurchaseOrderStatus.ISSUED);
        po.setStatus(PurchaseOrderStatus.ISSUED);
        po.setIssuedBy(userRepository.getReferenceById(SecurityUtils.getCurrentUserId()));
        po.setIssuedAt(clock.instant());
        var saved = poRepository.save(po);

        notificationService.createForVendorUsers(po.getVendor().getId(),
                NotificationEvent.PURCHASE_ORDER_ISSUED, "PurchaseOrder", saved.getId(),
                "Purchase order issued",
                saved.getPoNumber() + " awaits your acknowledgement.");
        auditService.record(AuditAction.PURCHASE_ORDER_ISSUED, "PurchaseOrder", saved.getId(),
                null, saved.getStatus().name());
        return toDetailResponse(saved);
    }

    @Transactional
    public com.vendorsphere.purchaseorder.dto.PurchaseOrderResponse acknowledge(UUID poId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID vendorScope = vendorAccessGuard.currentVendorId()
                .orElseThrow(() -> new BusinessException("Access denied", HttpStatus.FORBIDDEN));
        var po = poRepository.findByIdAndOrganizationIdAndVendorId(poId, organizationId, vendorScope)
                .orElseThrow(() -> new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));

        transition(po.getStatus(), PurchaseOrderStatus.ACKNOWLEDGED);
        po.setStatus(PurchaseOrderStatus.ACKNOWLEDGED);
        po.setAcknowledgedAt(clock.instant());
        return toDetailResponse(poRepository.save(po));
    }

    @Transactional
    public com.vendorsphere.purchaseorder.dto.PurchaseOrderResponse close(UUID poId) {
        var po = findInternal(poId);
        if (po.getStatus() != PurchaseOrderStatus.DELIVERED) {
            throw new BusinessException(
                    "Cannot close a " + po.getStatus() + " purchase order", HttpStatus.CONFLICT);
        }
        po.setStatus(PurchaseOrderStatus.CLOSED);
        po.setClosedAt(clock.instant());
        var saved = poRepository.save(po);

        // Requirement 19.9: closing recalculates the vendor's performance.
        performanceEngine.recalculate(saved.getVendor().getId());
        return toDetailResponse(saved);
    }

    @Transactional
    public com.vendorsphere.purchaseorder.dto.PurchaseOrderResponse cancel(
            UUID poId, com.vendorsphere.purchaseorder.dto.PurchaseOrderCancelRequest request) {
        String reason = request == null || request.reason() == null
                ? null : request.reason().trim();
        if (reason == null || reason.isEmpty()) {
            throw new BusinessException(CANCELLATION_REASON_MESSAGE, HttpStatus.BAD_REQUEST);
        }
        var po = findInternal(poId);
        boolean deliveredAnything = poItemRepository
                .findByPurchaseOrderIdOrderByCreatedAtAscIdAsc(po.getId()).stream()
                .anyMatch(item -> item.getDeliveredQuantity().signum() > 0);
        if (deliveredAnything) {
            throw new BusinessException(DELIVERIES_EXIST_MESSAGE, HttpStatus.CONFLICT);
        }
        transition(po.getStatus(), PurchaseOrderStatus.CANCELLED);
        po.setStatus(PurchaseOrderStatus.CANCELLED);
        po.setCancellationReason(reason);
        var saved = poRepository.save(po);
        auditService.record(AuditAction.PURCHASE_ORDER_CANCELLED, "PurchaseOrder", saved.getId(),
                null, reason);
        return toDetailResponse(saved);
    }

    @Transactional
    public com.vendorsphere.purchaseorder.dto.PurchaseOrderResponse update(
            UUID poId, com.vendorsphere.purchaseorder.dto.PurchaseOrderUpdateRequest request) {
        var po = findInternal(poId);
        if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new BusinessException("Purchase order can only be changed while in DRAFT",
                    HttpStatus.CONFLICT);
        }
        po.setDeliveryAddress(request.deliveryAddress());
        po.setExpectedDelivery(request.expectedDelivery());
        po.setPaymentTerms(request.paymentTerms());
        po.setTermsConditions(request.termsConditions());
        return toDetailResponse(poRepository.save(po));
    }

    @Transactional(readOnly = true)
    public com.vendorsphere.purchaseorder.dto.PurchaseOrderResponse get(UUID poId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID vendorScope = vendorAccessGuard.currentVendorId().orElse(null);
        var po = vendorScope == null
                ? poRepository.findByIdAndOrganizationId(poId, organizationId)
                : poRepository.findByIdAndOrganizationIdAndVendorId(poId, organizationId, vendorScope);
        return toDetailResponse(po.orElseThrow(() ->
                new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public List<com.vendorsphere.purchaseorder.dto.PurchaseOrderResponse> list() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID vendorScope = vendorAccessGuard.currentVendorId().orElse(null);
        List<com.vendorsphere.purchaseorder.entity.PurchaseOrder> rows =
                poRepository.findByOrganizationId(organizationId);
        return rows.stream()
                .filter(po -> vendorScope == null
                        || (po.getVendor().getId().equals(vendorScope)
                                && po.getStatus() != PurchaseOrderStatus.DRAFT))
                .map(this::toDetailResponse)
                .toList();
    }

    // ----- helpers -----

    private void transition(PurchaseOrderStatus from, PurchaseOrderStatus to) {
        PurchaseOrderStatusTransitions.MACHINE.assertTransition(from, to);
    }

    private com.vendorsphere.purchaseorder.entity.PurchaseOrder findInternal(UUID poId) {
        return poRepository.findByIdAndOrganizationId(poId, SecurityUtils.getCurrentOrganizationId())
                .orElseThrow(() -> new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
    }

    private com.vendorsphere.purchaseorder.dto.PurchaseOrderResponse toDetailResponse(
            com.vendorsphere.purchaseorder.entity.PurchaseOrder po) {
        List<com.vendorsphere.purchaseorder.dto.PurchaseOrderResponse.ItemResponse> items =
                poItemRepository.findByPurchaseOrderIdOrderByCreatedAtAscIdAsc(po.getId())
                        .stream()
                        .map(com.vendorsphere.purchaseorder.dto.PurchaseOrderResponse.ItemResponse::from)
                        .toList();
        return com.vendorsphere.purchaseorder.dto.PurchaseOrderResponse.from(po, items);
    }
}
