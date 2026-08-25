package com.vendorsphere.invoice.service;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.service.AuditService;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.common.util.Money;
import com.vendorsphere.delivery.repository.DeliveryRepository;
import com.vendorsphere.invoice.InvoiceStatus;
import com.vendorsphere.invoice.InvoiceStatusTransitions;
import com.vendorsphere.invoice.MatchFindingType;
import com.vendorsphere.invoice.MatchResolutionState;
import com.vendorsphere.invoice.MatchStatus;
import com.vendorsphere.invoice.ThreeWayMatcher;
import com.vendorsphere.invoice.dto.InvoiceResponse;
import com.vendorsphere.invoice.entity.Invoice;
import com.vendorsphere.invoice.entity.InvoiceItem;
import com.vendorsphere.invoice.entity.MatchFinding;
import com.vendorsphere.invoice.repository.InvoiceItemRepository;
import com.vendorsphere.invoice.repository.InvoiceRepository;
import com.vendorsphere.invoice.repository.MatchFindingRepository;
import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.service.NotificationService;
import com.vendorsphere.purchaseorder.PurchaseOrderStatus;
import com.vendorsphere.purchaseorder.entity.PurchaseOrderItem;
import com.vendorsphere.purchaseorder.repository.PurchaseOrderItemRepository;
import com.vendorsphere.purchaseorder.repository.PurchaseOrderRepository;
import com.vendorsphere.user.RoleName;
import com.vendorsphere.user.repository.UserRepository;
import com.vendorsphere.vendor.service.VendorAccessGuard;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class InvoiceService {

    static final String DUPLICATE_NUMBER_MESSAGE = "Invoice number already exists for this vendor";

    static final String DUE_DATE_MESSAGE = "Due date must be on or after the invoice date";

    static final String OVERRIDE_JUSTIFICATION_MESSAGE = "Override justification is required";

    static final String REJECTION_REASON_MESSAGE = "Rejection reason is required";

    static final String NOT_FOUND_MESSAGE = "Invoice not found";
    static final String PO_NOT_FOUND_MESSAGE = "Purchase order not found";

    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderItemRepository poItemRepository;
    private final DeliveryRepository deliveryRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final MatchFindingRepository matchFindingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final VendorAccessGuard vendorAccessGuard;
    private final Clock clock;

    public InvoiceService(
            PurchaseOrderRepository poRepository,
            PurchaseOrderItemRepository poItemRepository,
            DeliveryRepository deliveryRepository,
            InvoiceRepository invoiceRepository,
            InvoiceItemRepository invoiceItemRepository,
            MatchFindingRepository matchFindingRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            AuditService auditService,
            VendorAccessGuard vendorAccessGuard,
            Clock clock
    ) {
        this.poRepository = poRepository;
        this.poItemRepository = poItemRepository;
        this.deliveryRepository = deliveryRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.matchFindingRepository = matchFindingRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.vendorAccessGuard = vendorAccessGuard;
        this.clock = clock;
    }

    @Transactional
    public InvoiceResponse submit(
            UUID poId, com.vendorsphere.invoice.dto.InvoiceSubmitRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        var po = poRepository.findByIdAndOrganizationId(poId, organizationId)
                .orElseThrow(() -> new BusinessException(PO_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        if (po.getStatus() == PurchaseOrderStatus.DRAFT
                || po.getStatus() == PurchaseOrderStatus.ISSUED
                || po.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new BusinessException(
                    "Cannot invoice a " + po.getStatus() + " purchase order", HttpStatus.CONFLICT);
        }
        if (invoiceRepository.existsByOrganizationIdAndVendorIdAndInvoiceNumber(
                organizationId, po.getVendor().getId(), request.invoiceNumber())) {
            throw new BusinessException(DUPLICATE_NUMBER_MESSAGE, HttpStatus.CONFLICT);
        }
        if (request.dueDate() != null && request.dueDate().isBefore(request.invoiceDate())) {
            throw new BusinessException(DUE_DATE_MESSAGE, HttpStatus.BAD_REQUEST);
        }

        // Vendor users may only invoice their own order; internal roles are tenant-scoped already.
        UUID vendorScope = vendorAccessGuard.currentVendorId().orElse(null);
        if (vendorScope != null && !po.getVendor().getId().equals(vendorScope)) {
            throw new BusinessException(PO_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND);
        }

        List<PurchaseOrderItem> orderItems =
                poItemRepository.findByPurchaseOrderIdOrderByCreatedAtAscIdAsc(po.getId());
        BigDecimal subtotal = Money.ZERO_MONEY;
        BigDecimal taxTotal = Money.ZERO_MONEY;
        for (var line : request.items()) {
            subtotal = subtotal.add(Money.multiply(line.quantity(), line.unitPrice()));
            taxTotal = taxTotal.add(line.taxAmount() == null
                    ? Money.ZERO_MONEY : line.taxAmount());
        }
        BigDecimal discount = request.discountAmount() == null
                ? Money.ZERO_MONEY : request.discountAmount();

        Invoice invoice = new Invoice();
        invoice.setOrganization(po.getOrganization());
        invoice.setPurchaseOrder(po);
        invoice.setVendor(po.getVendor());
        invoice.setInvoiceNumber(request.invoiceNumber());
        invoice.setInvoiceDate(request.invoiceDate());
        invoice.setDueDate(request.dueDate());
        invoice.setSubtotal(Money.money(subtotal));
        invoice.setTaxAmount(Money.money(taxTotal));
        invoice.setDiscountAmount(Money.money(discount));
        // Requirement 22.3: total = sum of line totals minus discount.
        invoice.setTotalAmount(Money.money(subtotal.add(taxTotal).subtract(discount)));
        invoice.setStatus(InvoiceStatus.SUBMITTED);
        invoice.setDocumentUrl(request.documentUrl());
        Invoice saved = invoiceRepository.save(invoice);

        for (var line : request.items()) {
            PurchaseOrderItem source = orderItems.stream()
                    .filter(item -> item.getId().equals(line.purchaseOrderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(
                            "Invoice line references an unknown purchase order item",
                            HttpStatus.BAD_REQUEST));
            InvoiceItem item = new InvoiceItem();
            item.setInvoice(saved);
            item.setSourceItem(source);
            item.setItemName(source.getItemName());
            item.setQuantity(Money.quantity(line.quantity()));
            item.setUnitPrice(Money.money(line.unitPrice()));
            item.setTaxAmount(line.taxAmount() == null
                    ? Money.ZERO_MONEY : Money.money(line.taxAmount()));
            item.setLineTotal(Money.money(subtotalOf(line)));
            invoiceItemRepository.save(item);
        }

        evaluateMatch(saved);

        notificationService.createForRole(organizationId, RoleName.FINANCE,
                NotificationEvent.INVOICE_SUBMITTED, "Invoice", saved.getId(),
                "Invoice submitted",
                saved.getInvoiceNumber() + " received against " + po.getPoNumber() + ".");
        auditService.record(AuditAction.INVOICE_SUBMITTED, "Invoice", saved.getId(),
                null, saved.getTotalAmount().toPlainString());

        return toDetailResponse(saved);
    }

    private BigDecimal subtotalOf(com.vendorsphere.invoice.dto.InvoiceSubmitRequest.ItemLine line) {
        return Money.multiply(line.quantity(), line.unitPrice())
                .add(line.taxAmount() == null ? Money.ZERO_MONEY : line.taxAmount());
    }

    @Transactional
    public void evaluateMatch(Invoice invoice) {
        var po = invoice.getPurchaseOrder();
        List<PurchaseOrderItem> orderItems =
                poItemRepository.findByPurchaseOrderIdOrderByCreatedAtAscIdAsc(po.getId());

        List<ThreeWayMatcher.ItemComparison> comparisons = new ArrayList<>();
        for (InvoiceItem line : invoiceItemRepository
                .findByInvoiceIdOrderByCreatedAtAscIdAsc(invoice.getId())) {
            PurchaseOrderItem source = line.getSourceItem();
            comparisons.add(new ThreeWayMatcher.ItemComparison(
                    line.getItemName(),
                    source != null ? source.getQuantity() : null,
                    source != null ? source.getDeliveredQuantity() : null,
                    line.getQuantity(),
                    source != null ? source.getUnitPrice() : null,
                    line.getUnitPrice()));
        }

        Optional<Invoice> duplicate = findDuplicateOfMatchedInvoice(invoice);

        List<ThreeWayMatcher.Finding> findings = ThreeWayMatcher.match(
                new ThreeWayMatcher.MatchInput(
                        po.getPoNumber(),
                        deliveryRepository.existsByPurchaseOrderId(po.getId()),
                        duplicate.isPresent(),
                        duplicate.map(Invoice::getInvoiceNumber).orElse(null),
                        comparisons));

        matchFindingRepository.deleteByInvoiceId(invoice.getId());
        boolean notifyException = false;
        for (ThreeWayMatcher.Finding finding : findings) {
            MatchFinding entity = new MatchFinding();
            entity.setInvoice(invoice);
            entity.setFindingType(finding.type());
            entity.setItemName(finding.itemName());
            entity.setExpectedValue(finding.expectedValue());
            entity.setActualValue(finding.actualValue());
            entity.setDetail(finding.detail());
            matchFindingRepository.save(entity);
            notifyException = true;
        }

        invoice.setMatchStatus(findings.isEmpty()
                ? MatchStatus.MATCHED
                : MatchStatus.valueOf(ThreeWayMatcher.matchStatus(findings).name()));

        if (notifyException) {
            notificationService.createForRole(invoice.getOrganization().getId(), RoleName.FINANCE,
                    NotificationEvent.INVOICE_MATCH_EXCEPTION_RAISED, "Invoice", invoice.getId(),
                    "Invoice match exception",
                    invoice.getInvoiceNumber() + ": " + invoice.getMatchStatus());
            notificationService.createForRole(invoice.getOrganization().getId(),
                    RoleName.PROCUREMENT_MANAGER,
                    NotificationEvent.INVOICE_MATCH_EXCEPTION_RAISED, "Invoice", invoice.getId(),
                    "Invoice match exception",
                    invoice.getInvoiceNumber() + ": " + invoice.getMatchStatus());
        }
        invoiceRepository.save(invoice);
    }

    private Optional<Invoice> findDuplicateOfMatchedInvoice(Invoice invoice) {
        for (Invoice other : invoiceRepository.findByPurchaseOrderId(
                invoice.getPurchaseOrder().getId())) {
            if (!other.getId().equals(invoice.getId())
                    && other.getStatus() != InvoiceStatus.SUBMITTED
                    && other.getMatchStatus() == MatchStatus.MATCHED
                    && sameLines(other, invoice)) {
                return Optional.of(other);
            }
        }
        return Optional.empty();
    }

    private boolean sameLines(Invoice a, Invoice b) {
        List<com.vendorsphere.invoice.dto.InvoiceResponse.ItemResponse2> aLines =
                linesOf(a.getId());
        List<com.vendorsphere.invoice.dto.InvoiceResponse.ItemResponse2> bLines =
                linesOf(b.getId());
        if (aLines.size() != bLines.size()) {
            return false;
        }
        for (int i = 0; i < aLines.size(); i++) {
            var x = aLines.get(i);
            var y = bLines.get(i);
            boolean sameSource = java.util.Objects.equals(x.sourcePoItemId(), y.sourcePoItemId());
            if (!sameSource
                    || x.quantity().compareTo(y.quantity()) != 0
                    || x.unitPrice().compareTo(y.unitPrice()) != 0) {
                return false;
            }
        }
        return true;
    }

    private List<com.vendorsphere.invoice.dto.InvoiceResponse.ItemResponse2> linesOf(UUID id) {
        return invoiceItemRepository.findByInvoiceIdOrderByCreatedAtAscIdAsc(id).stream()
                .map(item -> new com.vendorsphere.invoice.dto.InvoiceResponse.ItemResponse2(
                        item.getSourceItem() != null ? item.getSourceItem().getId() : null,
                        item.getQuantity(), item.getUnitPrice()))
                .toList();
    }

    @Transactional
    public void review(UUID invoiceId, boolean approve, String comments) {
        Invoice invoice = findInternal(invoiceId);
        if (!approve && (comments == null || comments.isBlank())) {
            throw new BusinessException(REJECTION_REASON_MESSAGE, HttpStatus.BAD_REQUEST);
        }
        if (approve) {
            assertNoUnresolvedFindings(invoice);
        }

        if (invoice.getStatus() == InvoiceStatus.SUBMITTED) {
            InvoiceStatusTransitions.MACHINE.assertTransition(
                    invoice.getStatus(), InvoiceStatus.UNDER_REVIEW);
            invoice.setStatus(InvoiceStatus.UNDER_REVIEW);
        }
        InvoiceStatus target = approve ? InvoiceStatus.APPROVED : InvoiceStatus.REJECTED;
        InvoiceStatusTransitions.MACHINE.assertTransition(invoice.getStatus(), target);
        invoice.setStatus(target);
        invoice.setReviewedBy(userRepository.getReferenceById(SecurityUtils.getCurrentUserId()));
        invoice.setReviewedAt(clock.instant());
        invoice.setReviewComments(comments == null ? null : comments.trim());
        Invoice saved = invoiceRepository.save(invoice);

        notificationService.createForVendorUsers(saved.getVendor().getId(),
                approve ? NotificationEvent.INVOICE_APPROVED : NotificationEvent.INVOICE_REJECTED,
                "Invoice", saved.getId(),
                approve ? "Invoice approved" : "Invoice rejected",
                saved.getInvoiceNumber() + (approve ? " was approved."
                        : " was rejected: " + saved.getReviewComments()));
        auditService.record(approve ? AuditAction.INVOICE_APPROVED : AuditAction.INVOICE_REJECTED,
                "Invoice", saved.getId(), null, saved.getStatus().name());
    }

    private void assertNoUnresolvedFindings(Invoice invoice) {
        Set<MatchFindingType> unresolved = new LinkedHashSet<>();
        for (MatchFinding finding : matchFindingRepository
                .findByInvoiceIdOrderByCreatedAtAscIdAsc(invoice.getId())) {
            if (finding.getResolutionState() == MatchResolutionState.UNRESOLVED) {
                unresolved.add(finding.getFindingType());
            }
        }
        if (!unresolved.isEmpty()) {
            throw new BusinessException(
                    "Invoice has unresolved match exceptions: "
                            + String.join(", ", unresolved.stream().map(Enum::name).toList()),
                    HttpStatus.CONFLICT);
        }
    }

    @Transactional
    public void overrideFinding(UUID invoiceId, UUID findingId, String justification) {
        if (justification == null || justification.isBlank()) {
            throw new BusinessException(OVERRIDE_JUSTIFICATION_MESSAGE, HttpStatus.BAD_REQUEST);
        }
        Invoice invoice = findInternal(invoiceId);
        MatchFinding finding = matchFindingRepository.findById(findingId)
                .filter(candidate -> candidate.getInvoice().getId().equals(invoice.getId()))
                .orElseThrow(() -> new BusinessException("Match finding not found",
                        HttpStatus.NOT_FOUND));

        finding.setResolutionState(MatchResolutionState.OVERRIDDEN);
        finding.setOverriddenBy(userRepository.getReferenceById(SecurityUtils.getCurrentUserId()));
        finding.setOverriddenAt(clock.instant());
        finding.setOverrideJustification(justification.trim());
        matchFindingRepository.save(finding);
        auditService.record(AuditAction.MATCH_FINDING_OVERRIDDEN, "Invoice", invoice.getId(),
                null, finding.getFindingType() + ": " + justification.trim());
    }

    // ----- reads -----

    @Transactional(readOnly = true)
    public InvoiceResponse get(UUID invoiceId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID vendorScope = vendorAccessGuard.currentVendorId().orElse(null);
        var found = vendorScope == null
                ? invoiceRepository.findByIdAndOrganizationId(invoiceId, organizationId)
                : invoiceRepository.findByIdAndOrganizationIdAndVendorId(
                        invoiceId, organizationId, vendorScope);
        Invoice invoice = found.orElseThrow(() ->
                new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        return toDetailResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> list() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID vendorScope = vendorAccessGuard.currentVendorId().orElse(null);
        return invoiceRepository.findByOrganizationId(organizationId).stream()
                .filter(invoice -> vendorScope == null
                        || invoice.getVendor().getId().equals(vendorScope))
                .map(this::toDetailResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse.MatchResult matchResult(UUID invoiceId) {
        Invoice invoice = findInternal(invoiceId);
        List<MatchFinding> findings =
                matchFindingRepository.findByInvoiceIdOrderByCreatedAtAscIdAsc(invoice.getId());
        List<InvoiceResponse.MatchItem> items = new ArrayList<>();
        for (InvoiceItem line : invoiceItemRepository
                .findByInvoiceIdOrderByCreatedAtAscIdAsc(invoice.getId())) {
            PurchaseOrderItem source = line.getSourceItem();
            items.add(new InvoiceResponse.MatchItem(
                    line.getId(),
                    line.getItemName(),
                    source != null ? source.getQuantity() : null,
                    source != null ? source.getDeliveredQuantity() : null,
                    line.getQuantity(),
                    source != null ? source.getUnitPrice() : null,
                    line.getUnitPrice()));
        }
        List<InvoiceResponse.FindingResponse> findingRows = findings.stream()
                .map(finding -> new InvoiceResponse.FindingResponse(
                        finding.getId(), finding.getFindingType(), finding.getItemName(),
                        finding.getExpectedValue(), finding.getActualValue(), finding.getDetail(),
                        finding.getResolutionState(), finding.getOverrideJustification()))
                .toList();
        return new InvoiceResponse.MatchResult(
                invoice.getId(), invoice.getMatchStatus(), findingRows, items);
    }

    // ----- helpers -----

    private Invoice findInternal(UUID invoiceId) {
        return invoiceRepository
                .findByIdAndOrganizationId(invoiceId, SecurityUtils.getCurrentOrganizationId())
                .orElseThrow(() -> new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
    }

    private InvoiceResponse toDetailResponse(Invoice invoice) {
        List<InvoiceResponse.ItemResponse> items = invoiceItemRepository
                .findByInvoiceIdOrderByCreatedAtAscIdAsc(invoice.getId()).stream()
                .map(item -> new InvoiceResponse.ItemResponse(
                        item.getId(),
                        item.getSourceItem() != null ? item.getSourceItem().getId() : null,
                        item.getItemName(), item.getQuantity(), item.getUnitPrice(),
                        item.getTaxAmount(), item.getLineTotal()))
                .toList();
        return InvoiceResponse.from(invoice, items);
    }
}
