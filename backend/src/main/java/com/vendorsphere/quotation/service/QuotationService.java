package com.vendorsphere.quotation.service;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.service.AuditService;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.common.util.Money;
import com.vendorsphere.common.util.ReferenceNumberGenerator;
import com.vendorsphere.common.util.ReferencePrefix;
import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.service.NotificationService;
import com.vendorsphere.quotation.QuotationCalculator;
import com.vendorsphere.quotation.EvaluationEngine;
import com.vendorsphere.quotation.QuotationStatus;
import com.vendorsphere.quotation.entity.Quotation;
import com.vendorsphere.quotation.entity.VendorEvaluation;
import com.vendorsphere.user.repository.UserRepository;
import com.vendorsphere.rfq.RfqStatus;
import com.vendorsphere.rfq.RfqVendorStatus;
import com.vendorsphere.rfq.entity.Rfq;
import com.vendorsphere.rfq.repository.RfqRepository;
import com.vendorsphere.rfq.repository.RfqVendorRepository;
import com.vendorsphere.user.RoleName;
import com.vendorsphere.vendor.service.VendorAccessGuard;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Quotation submission and revision by vendor users (Requirements 12, 13).
 *
 * <p>The submission window is OPEN-and-before-closing; at or after the closing instant everything
 * answers 409 {@code RFQ is closed for quotation submission} (Requirement 12.9). Every monetary
 * figure is computed server-side from the supplied primitives (Requirement 13); the request records
 * carry no total fields, so there is nothing to distrust.
 */
@Service
public class QuotationService {

    /** Pinned by Requirement 12.9. */
    static final String CLOSED_MESSAGE = "RFQ is closed for quotation submission";

    /** Pinned by Requirement 12.6. */
    static final String VALIDITY_MESSAGE =
            "Quotation validity date must be on or after the RFQ closing date";

    /** Pinned by Requirement 16.11, enforced by the weights service. */
    static final String WEIGHTS_SUM_MESSAGE = "Criteria weights must sum to 1.00";

    static final String NOT_FOUND_MESSAGE = "Quotation not found";
    static final String RFQ_NOT_FOUND_MESSAGE = "RFQ not found";

    private final RfqRepository rfqRepository;
    private final com.vendorsphere.rfq.repository.RfqItemRepository rfqItemRepository;
    private final RfqVendorRepository rfqVendorRepository;
    private final com.vendorsphere.quotation.repository.QuotationRepository quotationRepository;
    private final com.vendorsphere.quotation.repository.QuotationItemRepository
            quotationItemRepository;
    private final com.vendorsphere.vendor.repository.VendorRepository vendorRepository;
    private final ReferenceNumberGenerator referenceNumberGenerator;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final VendorAccessGuard vendorAccessGuard;
    private final Clock clock;
    private final com.vendorsphere.quotation.repository.VendorEvaluationRepository
            evaluationRepository;
    private final com.vendorsphere.vendor.repository.VendorRepository performanceVendorRepository;
    private final EvaluationCriteriaWeightService weightsService;
    private final UserRepository userRepository;

    public QuotationService(
            RfqRepository rfqRepository,
            com.vendorsphere.rfq.repository.RfqItemRepository rfqItemRepository,
            RfqVendorRepository rfqVendorRepository,
            com.vendorsphere.quotation.repository.QuotationRepository quotationRepository,
            com.vendorsphere.quotation.repository.QuotationItemRepository quotationItemRepository,
            com.vendorsphere.vendor.repository.VendorRepository vendorRepository,
            ReferenceNumberGenerator referenceNumberGenerator,
            NotificationService notificationService,
            AuditService auditService,
            VendorAccessGuard vendorAccessGuard,
            Clock clock,
            com.vendorsphere.quotation.repository.VendorEvaluationRepository evaluationRepository,
            com.vendorsphere.vendor.repository.VendorRepository performanceVendorRepository,
            EvaluationCriteriaWeightService weightsService,
            UserRepository userRepository
    ) {
        this.rfqRepository = rfqRepository;
        this.rfqItemRepository = rfqItemRepository;
        this.rfqVendorRepository = rfqVendorRepository;
        this.quotationRepository = quotationRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.vendorRepository = vendorRepository;
        this.referenceNumberGenerator = referenceNumberGenerator;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.vendorAccessGuard = vendorAccessGuard;
        this.clock = clock;
        this.evaluationRepository = evaluationRepository;
        this.performanceVendorRepository = performanceVendorRepository;
        this.weightsService = weightsService;
        this.userRepository = userRepository;
    }

    /**
     * Submits or revises the calling vendor's quotation for an OPEN RFQ (Requirements 12.1, 12.8):
     * one priced line per RFQ item (Requirement 12.2), computed totals on every write, status
     * SUBMITTED with the submission instant, invitation moved to RESPONDED and officers notified
     * (Requirement 12.7).
     */
    @Transactional
    public com.vendorsphere.quotation.dto.QuotationResponse submit(
            UUID rfqId, com.vendorsphere.quotation.dto.QuotationSubmitRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID vendorId = currentVendorId();

        Rfq rfq = rfqRepository.findByIdAndOrganizationId(rfqId, organizationId)
                .orElseThrow(() -> new BusinessException(
                        RFQ_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        assertWindowOpen(rfq);
        assertInvited(rfq.getId(), vendorId);
        validateRanges(request);

        List<com.vendorsphere.rfq.entity.RfqItem> rfqItems =
                rfqItemRepository.findByRfqIdOrderBySortOrderAscIdAsc(rfq.getId());
        List<String> unpriced = rfqItems.stream()
                .filter(item -> request.items().stream()
                        .noneMatch(line -> item.getId().equals(line.rfqItemId())))
                .map(com.vendorsphere.rfq.entity.RfqItem::getItemName)
                .toList();
        if (!unpriced.isEmpty()) {
            throw new BusinessException(unpricedMessage(unpriced), HttpStatus.BAD_REQUEST);
        }

        boolean revision = false;
        Quotation quotation = quotationRepository.findByRfqIdAndVendorId(rfq.getId(), vendorId)
                .orElse(null);
        if (quotation == null) {
            quotation = new Quotation();
            quotation.setRfq(rfq);
            quotation.setVendor(vendorRepository.getReferenceById(vendorId));
            quotation.setQuotationNumber(referenceNumberGenerator.allocate(
                    organizationId, ReferencePrefix.QUOT));
        } else {
            revision = quotation.getStatus() == QuotationStatus.SUBMITTED;
        }

        applyFigures(quotation, request, rfqItems, rfq);
        quotation.setStatus(QuotationStatus.SUBMITTED);
        quotation.setSubmittedAt(clock.instant());
        Quotation saved = quotationRepository.save(quotation);

        persistItems(saved.getId(), request.items(), rfqItems);

        rfqVendorRepository.findByRfqIdAndVendorId(rfq.getId(), vendorId).ifPresent(invitation -> {
            invitation.setStatus(RfqVendorStatus.RESPONDED);
            rfqVendorRepository.save(invitation);
        });

        notificationService.createForRole(organizationId, RoleName.PROCUREMENT_OFFICER,
                NotificationEvent.QUOTATION_SUBMITTED, "Quotation", saved.getId(),
                "Quotation submitted",
                saved.getQuotationNumber() + " received for " + rfq.getRfqNumber() + ".");
        auditService.record(revision ? AuditAction.QUOTATION_REVISED : AuditAction.QUOTATION_SUBMITTED,
                "Quotation", saved.getId(), null, saved.getTotalAmount());

        return toDetailResponse(saved);
    }

    // ----- helpers -----

    private void assertWindowOpen(Rfq rfq) {
        if (rfq.getStatus() != RfqStatus.OPEN
                || !clock.instant().isBefore(rfq.getClosingDate())) {
            throw new BusinessException(CLOSED_MESSAGE, HttpStatus.CONFLICT);
        }
    }

    private void assertInvited(UUID rfqId, UUID vendorId) {
        if (rfqVendorRepository.findByRfqIdAndVendorId(rfqId, vendorId).isEmpty()) {
            throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
        }
    }

    /** Requirement 12.5: field ranges, each with its own message. */
    private void validateRanges(com.vendorsphere.quotation.dto.QuotationSubmitRequest request) {
        for (var line : request.items()) {
            require(line.unitPrice() == null || line.unitPrice().signum() >= 0,
                    "Unit price must be greater than or equal to zero");
            require(line.taxRate() == null
                            || (line.taxRate().compareTo(BigDecimal.ZERO) >= 0
                            && line.taxRate().compareTo(new BigDecimal("100")) <= 0),
                    "Tax rate must be between 0 and 100");
            require(line.discountAmount() == null || line.discountAmount().signum() >= 0,
                    "Discount amount must be greater than or equal to zero");
        }
        require(request.shippingAmount() == null || request.shippingAmount().signum() >= 0,
                "Shipping amount must be greater than or equal to zero");
        require(request.deliveryPeriodDays() == null || request.deliveryPeriodDays() >= 0,
                "Delivery period in days must be zero or more");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(message, HttpStatus.BAD_REQUEST);
        }
    }

    /** Requirement 12.3: the 400 lists every RFQ item name that carries no price. */
    static String unpricedMessage(List<String> names) {
        Set<String> distinct = new LinkedHashSet<>(names);
        return "No price supplied for: " + String.join(", ", distinct);
    }

    private UUID currentVendorId() {
        // A vendor user without a linked vendor fails closed (Requirement 30.8).
        return vendorAccessGuard.currentVendorId()
                .orElseThrow(() -> new BusinessException("Access denied", HttpStatus.FORBIDDEN));
    }

    private void applyFigures(
            Quotation quotation,
            com.vendorsphere.quotation.dto.QuotationSubmitRequest request,
            List<com.vendorsphere.rfq.entity.RfqItem> rfqItems,
            Rfq rfq) {
        List<QuotationCalculator.ItemInput> inputs = new ArrayList<>();
        for (var line : request.items()) {
            inputs.add(new QuotationCalculator.ItemInput(
                    line.quantity(), line.unitPrice(),
                    line.taxRate() == null ? BigDecimal.ZERO : line.taxRate(),
                    line.discountAmount()));
        }
        QuotationCalculator.Totals totals =
                QuotationCalculator.compute(inputs, request.shippingAmount());
        quotation.setSubtotal(totals.subtotal());
        quotation.setTaxAmount(totals.taxAmount());
        quotation.setDiscountAmount(totals.discountAmount());
        quotation.setShippingAmount(Money.money(request.shippingAmount()));
        quotation.setTotalAmount(totals.totalAmount());
        quotation.setDeliveryPeriodDays(request.deliveryPeriodDays());
        quotation.setPaymentTerms(request.paymentTerms());
        quotation.setWarranty(request.warranty());
        quotation.setWarrantyMonths(request.warrantyMonths());

        // Requirement 12.6: validity may not expire before the window closes.
        if (request.validityDate() != null) {
            LocalDate closingDay = java.time.LocalDate.ofInstant(
                    rfq.getClosingDate(), java.time.ZoneOffset.UTC);
            if (request.validityDate().isBefore(closingDay)) {
                throw new BusinessException(VALIDITY_MESSAGE, HttpStatus.BAD_REQUEST);
            }
        }
        quotation.setValidityDate(request.validityDate());
        quotation.setNotes(request.notes());
    }

    private void persistItems(
            UUID quotationId,
            List<com.vendorsphere.quotation.dto.QuotationSubmitRequest.ItemLine> lines,
            List<com.vendorsphere.rfq.entity.RfqItem> rfqItems) {
        quotationItemRepository.deleteByQuotationId(quotationId);
        var quotationRef = quotationRepository.getReferenceById(quotationId);
        for (var line : lines) {
            com.vendorsphere.rfq.entity.RfqItem source = rfqItems.stream()
                    .filter(item -> item.getId().equals(line.rfqItemId()))
                    .findFirst()
                    .orElseThrow();
            QuotationCalculator.ItemTotals totals = QuotationCalculator.computeItem(
                    new QuotationCalculator.ItemInput(line.quantity(), line.unitPrice(),
                            line.taxRate() == null ? BigDecimal.ZERO : line.taxRate(),
                            line.discountAmount()));

            com.vendorsphere.quotation.entity.QuotationItem item =
                    new com.vendorsphere.quotation.entity.QuotationItem();
            item.setQuotation(quotationRef);
            item.setSourceItem(source);
            item.setItemName(source.getItemName());
            item.setQuantity(Money.quantity(line.quantity()));
            item.setUnitPrice(Money.money(line.unitPrice()));
            item.setTaxRate(line.taxRate() == null ? Money.ZERO_MONEY : line.taxRate());
            item.setTaxAmount(totals.taxAmount());
            item.setDiscountAmount(line.discountAmount() == null
                    ? Money.ZERO_MONEY
                    : Money.money(line.discountAmount()));
            item.setLineTotal(totals.lineTotal());
            quotationItemRepository.save(item);
        }
    }

    private com.vendorsphere.quotation.dto.QuotationResponse toDetailResponse(Quotation quotation) {
        List<com.vendorsphere.quotation.dto.QuotationResponse.ItemResponse> items =
                quotationItemRepository
                        .findByQuotationIdOrderByCreatedAtAscIdAsc(quotation.getId()).stream()
                        .map(com.vendorsphere.quotation.dto.QuotationResponse.ItemResponse::from)
                        .toList();
        return com.vendorsphere.quotation.dto.QuotationResponse.from(quotation, items);
    }

    // ----- reads, comparison and evaluation -----

    /**
     * One quotation with the confidentiality rules of Requirement 14 applied: a vendor user sees only
     * its own quotation - anything else is 404 {@code Quotation not found} - and an internal user
     * sees prices only once the RFQ is past OPEN.
     */
    @Transactional(readOnly = true)
    public com.vendorsphere.quotation.dto.QuotationResponse get(UUID quotationId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID vendorScope = vendorAccessGuard.currentVendorId().orElse(null);
        java.util.Optional<Quotation> found = vendorScope == null
                ? quotationRepository.findByIdAndRfqOrganizationId(quotationId, organizationId)
                : quotationRepository.findByIdAndRfqOrganizationIdAndVendorId(
                        quotationId, organizationId, vendorScope);
        Quotation quotation = found.orElseThrow(() ->
                new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        if (vendorScope == null && quotation.getRfq().getStatus() == RfqStatus.OPEN) {
            return com.vendorsphere.quotation.dto.QuotationResponse.redacted(quotation);
        }
        return toDetailResponse(quotation);
    }

    /**
     * Every quotation of one RFQ for internal readers; while the RFQ is OPEN the rows are redacted -
     * identity and status without prices - so officers can watch response counts without seeing bids
     * early (Requirement 14.5).
     */
    @Transactional(readOnly = true)
    public List<com.vendorsphere.quotation.dto.QuotationResponse> listForRfq(UUID rfqId) {
        Rfq rfq = rfqRepository.findByIdAndOrganizationId(rfqId,
                        SecurityUtils.getCurrentOrganizationId())
                .orElseThrow(() -> new BusinessException(RFQ_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        boolean open = rfq.getStatus() == RfqStatus.OPEN;
        return quotationRepository.findByRfqId(rfq.getId()).stream()
                .map(quotation -> open
                        ? com.vendorsphere.quotation.dto.QuotationResponse.redacted(quotation)
                        : toDetailResponse(quotation))
                .toList();
    }

    /**
     * The normalized comparison of a closed RFQ (Requirement 15): one row per qualifying quotation,
     * ordered by evaluation score descending then total ascending. DRAFT/OPEN requests are refused -
     * ranking half-open bidding would expose in-flight prices.
     */
    @Transactional(readOnly = true)
    public com.vendorsphere.quotation.dto.ComparisonResponse compare(UUID rfqId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Rfq rfq = rfqRepository.findByIdAndOrganizationId(rfqId, organizationId)
                .orElseThrow(() -> new BusinessException(RFQ_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        if (rfq.getStatus() == RfqStatus.DRAFT || rfq.getStatus() == RfqStatus.OPEN) {
            throw new BusinessException(COMPARISON_CLOSED_MESSAGE, HttpStatus.CONFLICT);
        }

        List<com.vendorsphere.rfq.entity.RfqItem> rfqItems =
                rfqItemRepository.findByRfqIdOrderBySortOrderAscIdAsc(rfq.getId());
        var evaluations = evaluationRepository.findByRfqId(rfq.getId()).stream()
                .collect(java.util.stream.Collectors.toMap(
                        evaluation -> evaluation.getQuotation().getId(),
                        java.util.function.Function.identity()));

        List<com.vendorsphere.quotation.dto.ComparisonResponse.ComparisonRow> rows =
                new ArrayList<>();
        for (Quotation quotation : quotationRepository.findByRfqId(rfq.getId())) {
            if (!COMPARABLE_STATUSES.contains(quotation.getStatus())) {
                continue;
            }
            var evaluation = evaluations.get(quotation.getId());
            List<com.vendorsphere.quotation.entity.QuotationItem> quotedItems =
                    quotationItemRepository.findByQuotationIdOrderByCreatedAtAscIdAsc(
                            quotation.getId());
            List<com.vendorsphere.quotation.dto.ComparisonResponse.ItemRow> itemRows =
                    rfqItems.stream().map(rfqItem -> {
                        com.vendorsphere.quotation.entity.QuotationItem match = quotedItems.stream()
                                .filter(quoted -> quoted.getSourceItem() != null
                                        && rfqItem.getId()
                                                .equals(quoted.getSourceItem().getId()))
                                .findFirst()
                                .orElse(null);
                        return new com.vendorsphere.quotation.dto.ComparisonResponse.ItemRow(
                                rfqItem.getId(), rfqItem.getItemName(), rfqItem.getQuantity(),
                                match != null ? match.getUnitPrice() : null,
                                match != null ? match.getLineTotal() : null);
                    }).toList();

            rows.add(new com.vendorsphere.quotation.dto.ComparisonResponse.ComparisonRow(
                    quotation.getId(),
                    quotation.getVendor().getId(),
                    quotation.getVendor().getCompanyName(),
                    performanceScoreOf(quotation),
                    quotation.getSubtotal(),
                    quotation.getTaxAmount(),
                    quotation.getDiscountAmount(),
                    quotation.getShippingAmount(),
                    quotation.getTotalAmount(),
                    quotation.getDeliveryPeriodDays(),
                    quotation.getWarrantyMonths(),
                    quotation.getPaymentTerms(),
                    quotation.getValidityDate(),
                    evaluation != null ? evaluation.getId() : null,
                    evaluation != null ? evaluation.getTotalScore() : null,
                    evaluation != null && evaluation.isRecommended(),
                    itemRows));
        }

        // Requirement 15.6: score descending, then total ascending; unscored rows sink last.
        rows.sort(java.util.Comparator
                .<com.vendorsphere.quotation.dto.ComparisonResponse.ComparisonRow,
                        java.math.BigDecimal>comparing(
                        row -> row.evaluationScore() == null
                                ? BigDecimal.valueOf(-1) : row.evaluationScore(),
                        java.util.Comparator.reverseOrder())
                .thenComparing(com.vendorsphere.quotation.dto.ComparisonResponse.ComparisonRow
                        ::totalAmount));

        return new com.vendorsphere.quotation.dto.ComparisonResponse(
                rfq.getId(), rfq.getRfqNumber(), rfq.getStatus(), rows);
    }

    /**
     * Scores every qualifying quotation of an RFQ and persists one evaluation record per quotation,
     * leaving all statuses untouched (Requirements 16.13, 16.14). Vendor performance falls back to
     * 50.00 while no snapshot exists - the same "no history yet" default the engine defines for an
     * unproven vendor (Requirement 16.5).
     */
    @Transactional
    public void evaluate(UUID rfqId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Rfq rfq = rfqRepository.findByIdAndOrganizationId(rfqId, organizationId)
                .orElseThrow(() -> new BusinessException(RFQ_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));

        List<Quotation> quotations = quotationRepository.findByRfqId(rfq.getId()).stream()
                .filter(quotation -> COMPARABLE_STATUSES.contains(quotation.getStatus()))
                .toList();
        List<EvaluationEngine.Input> inputs = quotations.stream()
                .map(quotation -> new EvaluationEngine.Input(
                        quotation.getId(),
                        quotation.getVendor().getId(),
                        quotation.getTotalAmount(),
                        quotation.getDeliveryPeriodDays(),
                        quotation.getWarrantyMonths(),
                        performanceScoreOf(quotation)))
                .toList();

        List<EvaluationEngine.Result> results = EvaluationEngine.evaluate(
                inputs, weightsService.resolve());
        var evaluator = userRepository.getReferenceById(SecurityUtils.getCurrentUserId());
        for (EvaluationEngine.Result result : results) {
            Quotation quotation = quotations.stream()
                    .filter(q -> q.getId().equals(result.quotationId()))
                    .findFirst()
                    .orElseThrow();
            VendorEvaluation evaluation = evaluationRepository
                    .findByRfqIdAndQuotationId(rfq.getId(), quotation.getId())
                    .orElseGet(() -> {
                        VendorEvaluation created = new VendorEvaluation();
                        created.setRfq(rfq);
                        created.setQuotation(quotation);
                        created.setVendor(quotation.getVendor());
                        return created;
                    });
            evaluation.setPriceScore(result.priceScore());
            evaluation.setDeliveryScore(result.deliveryScore());
            evaluation.setWarrantyScore(result.warrantyScore());
            evaluation.setPerformanceScore(result.performanceScore());
            evaluation.setTotalScore(result.evaluationScore());
            evaluation.setRecommended(result.recommended());
            evaluation.setEvaluatedBy(evaluator);
            evaluation.setEvaluatedAt(clock.instant());
            evaluationRepository.save(evaluation);
        }
    }

    private static final String COMPARISON_CLOSED_MESSAGE =
            "Comparison is available after the RFQ closes";

    private static final Set<QuotationStatus> COMPARABLE_STATUSES =
            java.util.Set.of(QuotationStatus.SUBMITTED, QuotationStatus.UNDER_REVIEW,
                    QuotationStatus.SELECTED, QuotationStatus.REJECTED);

    /** Latest snapshot score, or the 50.00 default of Requirement 16.5 while none exists. */
    private BigDecimal performanceScoreOf(Quotation quotation) {
        return performanceVendorRepository
                .findLatestPerformanceScore(quotation.getVendor().getId())
                .map(Money::clampScore)
                .orElse(new BigDecimal("50.00"));
    }
}
