package com.vendorsphere.quotation.service;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.service.AuditService;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.service.NotificationService;
import com.vendorsphere.quotation.QuotationStatus;
import com.vendorsphere.quotation.entity.Quotation;
import com.vendorsphere.quotation.entity.VendorEvaluation;
import com.vendorsphere.quotation.entity.VendorSelection;
import com.vendorsphere.quotation.repository.QuotationRepository;
import com.vendorsphere.quotation.repository.VendorEvaluationRepository;
import com.vendorsphere.quotation.repository.VendorSelectionRepository;
import com.vendorsphere.rfq.RfqStatus;
import com.vendorsphere.rfq.RfqVendorStatus;
import com.vendorsphere.rfq.entity.Rfq;
import com.vendorsphere.rfq.repository.RfqRepository;
import com.vendorsphere.rfq.repository.RfqVendorRepository;
import com.vendorsphere.user.RoleName;
import com.vendorsphere.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Vendor selection and award (Requirement 17).
 *
 * <p>One transaction flips the whole award state: the target quotation to SELECTED, every other
 * in-flight quotation to REJECTED, the RFQ to AWARDED and the winning invitation to AWARDED
 * (Requirements 17.1, 17.10). The decision is never implicit - the evaluation engine's recommended
 * flag only advises; an explicit select request with a justification is the only path here
 * (Requirement 17.7).
 */
@Service
public class SelectionService {

    /** Pinned by Requirement 17.3. */
    static final String JUSTIFICATION_MESSAGE = "Selection justification is required";

    /** Pinned by Requirement 17.4. */
    static final String ALREADY_AWARDED_MESSAGE = "RFQ is already awarded";

    static final String RFQ_NOT_FOUND_MESSAGE = "RFQ not found";
    static final String QUOTATION_NOT_FOUND_MESSAGE = "Quotation not found";

    private final RfqRepository rfqRepository;
    private final RfqVendorRepository rfqVendorRepository;
    private final QuotationRepository quotationRepository;
    private final VendorSelectionRepository selectionRepository;
    private final VendorEvaluationRepository evaluationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final com.vendorsphere.analytics.service.PerformanceEngine performanceEngine;
    private final Clock clock;

    public SelectionService(
            RfqRepository rfqRepository,
            RfqVendorRepository rfqVendorRepository,
            QuotationRepository quotationRepository,
            VendorSelectionRepository selectionRepository,
            VendorEvaluationRepository evaluationRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            AuditService auditService,
            com.vendorsphere.analytics.service.PerformanceEngine performanceEngine,
            Clock clock
    ) {
        this.rfqRepository = rfqRepository;
        this.rfqVendorRepository = rfqVendorRepository;
        this.quotationRepository = quotationRepository;
        this.selectionRepository = selectionRepository;
        this.evaluationRepository = evaluationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.performanceEngine = performanceEngine;
        this.clock = clock;
    }

    /**
     * Awards an RFQ to one quotation (Requirement 17.1).
     *
     * <p>A CLOSED RFQ walks CLOSED&rarr;EVALUATION&rarr;AWARDED internally - the same pass-through
     * pattern the purchase request review uses, because no separate "start evaluation" endpoint
     * exists. A WITHDRAWN or REJECTED target is refused with a message naming its status
     * (Requirement 17.5). Every invited vendor's users learn the outcome (Requirement 17.8).
     */
    @Transactional
    public void select(UUID rfqId, UUID quotationId, String justification) {
        String reason = justification == null || justification.isBlank()
                ? null
                : justification.trim();
        if (reason == null) {
            throw new BusinessException(JUSTIFICATION_MESSAGE, HttpStatus.BAD_REQUEST);
        }
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Rfq rfq = rfqRepository.findByIdAndOrganizationId(rfqId, organizationId)
                .orElseThrow(() -> new BusinessException(
                        RFQ_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        if (selectionRepository.existsByRfqId(rfq.getId())
                || rfq.getStatus() == RfqStatus.AWARDED) {
            throw new BusinessException(ALREADY_AWARDED_MESSAGE, HttpStatus.CONFLICT);
        }

        Quotation winner = quotationRepository.findByIdAndRfqOrganizationId(
                quotationId, organizationId)
                .filter(quotation -> quotation.getRfq().getId().equals(rfq.getId()))
                .orElseThrow(() -> new BusinessException(
                        QUOTATION_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        if (winner.getStatus() == QuotationStatus.WITHDRAWN
                || winner.getStatus() == QuotationStatus.REJECTED) {
            throw new BusinessException(
                    "Cannot select a " + winner.getStatus() + " quotation", HttpStatus.CONFLICT);
        }
        // Walk CLOSED through EVALUATION first; see the javadoc above.
        if (rfq.getStatus() == RfqStatus.CLOSED) {
            rfq.setStatus(RfqStatus.EVALUATION);
        }
        rfq.setStatus(RfqStatus.AWARDED);

        List<Quotation> quotations = quotationRepository.findByRfqId(rfq.getId());
        for (Quotation quotation : quotations) {
            if (quotation.getId().equals(winner.getId())) {
                quotation.setStatus(QuotationStatus.SELECTED);
            } else if (quotation.getStatus() == QuotationStatus.SUBMITTED
                    || quotation.getStatus() == QuotationStatus.UNDER_REVIEW) {
                quotation.setStatus(QuotationStatus.REJECTED);
            }
        }

        VendorSelection selection = new VendorSelection();
        selection.setRfq(rfq);
        selection.setQuotation(winner);
        selection.setVendor(winner.getVendor());
        selection.setSelectedBy(userRepository.getReferenceById(SecurityUtils.getCurrentUserId()));
        selection.setJustification(reason);
        selection.setSelectedAt(clock.instant());
        selectionRepository.save(selection);

        rfqVendorRepository.findByRfqIdOrderByInvitedAtAsc(rfq.getId()).forEach(invitation -> {
            if (invitation.getVendor().getId().equals(winner.getVendor().getId())) {
                invitation.setStatus(RfqVendorStatus.AWARDED);
                rfqVendorRepository.save(invitation);
            }
        });

        auditService.record(AuditAction.VENDOR_SELECTED, "Rfq", rfq.getId(), null,
                "quotation " + winner.getId() + ": " + reason);

        // Requirement 26.9: an award recalculates the winning vendor's performance.
        performanceEngine.recalculate(winner.getVendor().getId());

        for (var invitation : rfqVendorRepository.findByRfqIdOrderByInvitedAtAsc(rfq.getId())) {
            boolean won = invitation.getVendor().getId().equals(winner.getVendor().getId());
            notificationService.createForVendorUsers(invitation.getVendor().getId(),
                    NotificationEvent.VENDOR_SELECTED, "Rfq", rfq.getId(),
                    won ? "Your quotation was selected" : "RFQ awarded",
                    rfq.getRfqNumber() + ": "
                            + (won ? "your quotation was selected." : "another vendor was selected."));
        }
    }

    /**
     * Persists procurement comments on the evaluation record of one quotation (Requirement 17.6),
     * creating a minimal record when the RFQ has not been evaluated yet.
     */
    @Transactional
    public void comment(UUID quotationId, String text) {
        String comments = text == null || text.isBlank() ? null : text.trim();
        if (comments == null) {
            throw new BusinessException("Comment must not be blank", HttpStatus.BAD_REQUEST);
        }
        Quotation quotation = quotationRepository
                .findByIdAndRfqOrganizationId(quotationId, SecurityUtils.getCurrentOrganizationId())
                .orElseThrow(() -> new BusinessException(
                        QUOTATION_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));

        VendorEvaluation evaluation = evaluationRepository
                .findByRfqIdAndQuotationId(quotation.getRfq().getId(), quotation.getId())
                .orElseGet(() -> {
                    VendorEvaluation created = new VendorEvaluation();
                    created.setRfq(quotation.getRfq());
                    created.setQuotation(quotation);
                    created.setVendor(quotation.getVendor());
                    return created;
                });
        evaluation.setComments(comments);
        evaluation.setEvaluatedBy(
                userRepository.getReferenceById(SecurityUtils.getCurrentUserId()));
        evaluation.setEvaluatedAt(clock.instant());
        evaluationRepository.save(evaluation);
    }
}
