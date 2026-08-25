package com.vendorsphere.rfq.service;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.service.AuditService;
import com.vendorsphere.common.dto.PageResponse;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.common.util.Money;
import com.vendorsphere.common.util.ReferenceNumberGenerator;
import com.vendorsphere.common.util.ReferencePrefix;
import com.vendorsphere.common.util.SortWhitelist;
import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.service.NotificationService;
import com.vendorsphere.procurement.PurchaseRequestStatus;
import com.vendorsphere.procurement.entity.PurchaseRequest;
import com.vendorsphere.procurement.entity.PurchaseRequestItem;
import com.vendorsphere.procurement.repository.PurchaseRequestItemRepository;
import com.vendorsphere.procurement.repository.PurchaseRequestRepository;
import com.vendorsphere.rfq.RfqStatus;
import com.vendorsphere.rfq.RfqStatusTransitions;
import com.vendorsphere.rfq.dto.RfqCancelRequest;
import com.vendorsphere.rfq.dto.RfqCreateRequest;
import com.vendorsphere.rfq.dto.RfqItemRequest;
import com.vendorsphere.rfq.dto.RfqResponse;
import com.vendorsphere.rfq.dto.RfqSearchCriteria;
import com.vendorsphere.rfq.dto.RfqUpdateRequest;
import com.vendorsphere.rfq.entity.Rfq;
import com.vendorsphere.rfq.entity.RfqItem;
import com.vendorsphere.rfq.repository.RfqItemRepository;
import com.vendorsphere.rfq.repository.RfqRepository;
import com.vendorsphere.rfq.repository.RfqVendorRepository;
import com.vendorsphere.rfq.repository.RfqSpecifications;
import com.vendorsphere.common.util.PageSupport;
import com.vendorsphere.user.RoleName;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class RfqService {

    static final String DATES_MESSAGE = "Closing date must be after opening date";

    static final String CANCELLATION_REASON_MESSAGE = "Cancellation reason is required";

    static final String AWARDED_CANCEL_MESSAGE = "Awarded RFQ cannot be cancelled";

    static final String NO_INVITEES_MESSAGE = "RFQ requires at least one invited vendor";

    static final String QUANTITY_MESSAGE = "Quantity must be greater than zero";

    static final String NOT_FOUND_MESSAGE = "RFQ not found";
    static final String ITEM_NOT_FOUND_MESSAGE = "RFQ item not found";

    static String notFromStatusMessage(PurchaseRequestStatus status) {
        return "Cannot create an RFQ from a " + status + " purchase request";
    }

    public static final SortWhitelist SORTABLE =
            SortWhitelist.of("createdAt", "closingDate", "status");

    private final RfqRepository rfqRepository;
    private final RfqItemRepository rfqItemRepository;
    private final RfqVendorRepository rfqVendorRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestItemRepository purchaseRequestItemRepository;
    private final UserRepository userRepository;
    private final ReferenceNumberGenerator referenceNumberGenerator;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final Clock clock;

    public RfqService(
            RfqRepository rfqRepository,
            RfqItemRepository rfqItemRepository,
            RfqVendorRepository rfqVendorRepository,
            PurchaseRequestRepository purchaseRequestRepository,
            PurchaseRequestItemRepository purchaseRequestItemRepository,
            UserRepository userRepository,
            ReferenceNumberGenerator referenceNumberGenerator,
            NotificationService notificationService,
            AuditService auditService,
            Clock clock
    ) {
        this.rfqRepository = rfqRepository;
        this.rfqItemRepository = rfqItemRepository;
        this.rfqVendorRepository = rfqVendorRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.purchaseRequestItemRepository = purchaseRequestItemRepository;
        this.userRepository = userRepository;
        this.referenceNumberGenerator = referenceNumberGenerator;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public RfqResponse create(RfqCreateRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        assertDates(request.openingDate(), request.closingDate());

        PurchaseRequest source = purchaseRequestRepository
                .findByIdAndOrganizationId(request.purchaseRequestId(), organizationId)
                .orElseThrow(() -> new BusinessException(
                        "Purchase request not found", HttpStatus.NOT_FOUND));
        if (source.getStatus() != PurchaseRequestStatus.APPROVED
                && source.getStatus() != PurchaseRequestStatus.PROCUREMENT_STARTED) {
            throw new BusinessException(
                    notFromStatusMessage(source.getStatus()), HttpStatus.CONFLICT);
        }

        Rfq rfq = new Rfq();
        rfq.setOrganization(source.getOrganization());
        rfq.setPurchaseRequest(source);
        rfq.setCreatedBy(userRepository.getReferenceById(SecurityUtils.getCurrentUserId()));
        rfq.setRfqNumber(referenceNumberGenerator.allocate(organizationId, ReferencePrefix.RFQ));
        applyHeader(rfq, request.title(), request.description(), request.openingDate(),
                request.closingDate(), request.currency(), request.deliveryLocation(),
                request.terms());

        Rfq saved = rfqRepository.save(rfq);

        List<PurchaseRequestItem> sourceItems =
                purchaseRequestItemRepository.findByPurchaseRequestIdOrderBySortOrderAscIdAsc(
                        source.getId());
        for (PurchaseRequestItem sourceItem : sourceItems) {
            RfqItem item = new RfqItem();
            item.setRfq(saved);
            item.setSourceItemId(sourceItem.getId());
            item.setItemName(sourceItem.getItemName());
            item.setQuantity(Money.quantity(sourceItem.getQuantity()));
            item.setUnit(sourceItem.getUnit());
            item.setSpecification(sourceItem.getSpecification());
            item.setSortOrder(sourceItem.getSortOrder());
            rfqItemRepository.save(item);
        }

        if (source.getStatus() == PurchaseRequestStatus.APPROVED) {
            source.setStatus(PurchaseRequestStatus.PROCUREMENT_STARTED);
            purchaseRequestRepository.save(source);
        }

        auditService.record(AuditAction.RFQ_CREATED, "Rfq", saved.getId(), null,
                saved.getRfqNumber() + " from " + source.getRequestNumber());
        return toDetailResponse(saved);
    }

    @Transactional
    public RfqResponse update(UUID rfqId, RfqUpdateRequest request) {
        assertDates(request.openingDate(), request.closingDate());
        Rfq rfq = findDraft(rfqId);

        applyHeader(rfq, request.title(), request.description(), request.openingDate(),
                request.closingDate(), request.currency(), request.deliveryLocation(),
                request.terms());
        return toDetailResponse(rfqRepository.save(rfq));
    }

    private void applyHeader(
            Rfq rfq, String title, String description, java.time.Instant openingDate,
            java.time.Instant closingDate, String currency, String deliveryLocation,
            String terms) {
        rfq.setTitle(title);
        rfq.setDescription(description);
        rfq.setOpeningDate(openingDate);
        rfq.setClosingDate(closingDate);
        // An absent currency stores as the column default INR.
        rfq.setCurrency(currency == null || currency.isBlank() ? "INR" : currency.trim());
        rfq.setDeliveryLocation(deliveryLocation);
        rfq.setTerms(terms);
    }

    private void assertDates(java.time.Instant openingDate, java.time.Instant closingDate) {
        if (openingDate != null && closingDate != null
                && !closingDate.isAfter(openingDate)) {
            throw new BusinessException(DATES_MESSAGE, HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public RfqResponse addItem(UUID rfqId, RfqItemRequest request) {
        Rfq rfq = findDraft(rfqId);

        RfqItem item = new RfqItem();
        applyItem(item, request);
        item.setRfq(rfq);
        item.setSortOrder((int) rfqItemRepository.countByRfqId(rfq.getId()));

        rfqItemRepository.save(item);
        return toDetailResponse(rfq);
    }

    @Transactional
    public RfqResponse updateItem(UUID rfqId, UUID itemId, RfqItemRequest request) {
        findDraft(rfqId);
        RfqItem item = findItem(rfqId, itemId);

        applyItem(item, request);
        return toDetailResponse(item.getRfq());
    }

    @Transactional
    public RfqResponse removeItem(UUID rfqId, UUID itemId) {
        Rfq rfq = findDraft(rfqId);
        RfqItem item = findItem(rfqId, itemId);

        rfqItemRepository.delete(item);
        return toDetailResponse(rfq);
    }

    private void applyItem(RfqItem item, RfqItemRequest request) {
        if (request.quantity() == null || request.quantity().signum() <= 0) {
            throw new BusinessException(QUANTITY_MESSAGE, HttpStatus.BAD_REQUEST);
        }
        item.setItemName(request.itemName().trim());
        item.setQuantity(Money.quantity(request.quantity()));
        item.setUnit(request.unit() == null || request.unit().isBlank()
                ? "UNIT"
                : request.unit().trim());
        item.setSpecification(request.specification());
    }

    private RfqItem findItem(UUID rfqId, UUID itemId) {
        RfqItem item = rfqItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(
                        ITEM_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        if (!item.getRfq().getId().equals(rfqId)
                || !item.getRfq().getOrganization().getId()
                        .equals(SecurityUtils.getCurrentOrganizationId())) {
            throw new BusinessException(ITEM_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND);
        }
        return item;
    }

    @Transactional
    public RfqResponse open(UUID rfqId) {
        Rfq rfq = findInOrganization(rfqId);
        if (rfqVendorRepository.countByRfqId(rfq.getId()) < 1) {
            throw new BusinessException(NO_INVITEES_MESSAGE, HttpStatus.BAD_REQUEST);
        }
        transition(rfq.getStatus(), RfqStatus.OPEN);
        rfq.setStatus(RfqStatus.OPEN);
        return toDetailResponse(rfqRepository.save(rfq));
    }

    @Transactional
    public RfqResponse close(UUID rfqId) {
        Rfq rfq = findInOrganization(rfqId);
        transition(rfq.getStatus(), RfqStatus.CLOSED);
        rfq.setStatus(RfqStatus.CLOSED);
        return toDetailResponse(rfqRepository.save(rfq));
    }

    @Transactional
    public RfqResponse cancel(UUID rfqId, RfqCancelRequest request) {
        String reason = request == null || request.reason() == null
                ? null
                : request.reason().trim();
        if (reason == null || reason.isEmpty()) {
            throw new BusinessException(CANCELLATION_REASON_MESSAGE, HttpStatus.BAD_REQUEST);
        }
        Rfq rfq = findInOrganization(rfqId);
        if (rfq.getStatus() == RfqStatus.AWARDED) {
            throw new BusinessException(AWARDED_CANCEL_MESSAGE, HttpStatus.CONFLICT);
        }
        transition(rfq.getStatus(), RfqStatus.CANCELLED);

        int rejectedQuotations = rfqRepository.rejectInFlightQuotations(rfq.getId());
        List<UUID> invitedVendorIds = vendorIdsOf(rfq.getId());

        rfq.setStatus(RfqStatus.CANCELLED);
        rfq.setCancellationReason(reason);
        Rfq saved = rfqRepository.save(rfq);

        auditService.record(AuditAction.RFQ_CANCELLED, "Rfq", saved.getId(),
                rfq.getStatus(), reason + " (" + rejectedQuotations + " quotation(s) rejected)");

        for (UUID vendorId : invitedVendorIds) {
            notificationService.createForVendorUsers(vendorId,
                    NotificationEvent.RFQ_CANCELLED, "Rfq", saved.getId(),
                    "RFQ cancelled",
                    saved.getRfqNumber() + " was cancelled: " + reason);
        }
        return toDetailResponse(saved);
    }

    // ----- reads -----

    @Transactional(readOnly = true)
    public RfqResponse get(UUID rfqId) {
        return toDetailResponse(findInOrganization(rfqId));
    }

    @Transactional(readOnly = true)
    public PageResponse<RfqResponse> search(RfqSearchCriteria criteria, Pageable pageable) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Page<Rfq> page = rfqRepository.findAll(
                RfqSpecifications.search(organizationId, criteria), pageable);
        return PageSupport.map(page, this::toHeaderResponse);
    }

    // ----- helpers -----

    private Rfq findInOrganization(UUID rfqId) {
        return rfqRepository.findByIdAndOrganizationId(rfqId, SecurityUtils.getCurrentOrganizationId())
                .orElseThrow(() -> new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
    }

    private Rfq findDraft(UUID rfqId) {
        Rfq rfq = findInOrganization(rfqId);
        if (rfq.getStatus() != RfqStatus.DRAFT) {
            throw new BusinessException("RFQ can only be changed while in DRAFT",
                    HttpStatus.CONFLICT);
        }
        return rfq;
    }

    private void transition(RfqStatus from, RfqStatus to) {
        RfqStatusTransitions.MACHINE.assertTransition(from, to);
    }

    private List<UUID> vendorIdsOf(UUID rfqId) {
        return rfqVendorRepository.findByRfqIdOrderByInvitedAtAsc(rfqId).stream()
                .map(invitation -> invitation.getVendor().getId())
                .toList();
    }

    private RfqResponse toDetailResponse(Rfq rfq) {
        List<RfqResponse.RfqItemResponse> items = rfqItemRepository
                .findByRfqIdOrderBySortOrderAscIdAsc(rfq.getId()).stream()
                .map(RfqResponse.RfqItemResponse::from)
                .toList();
        List<RfqResponse.RfqVendorResponse> vendors = rfqVendorRepository
                .findByRfqIdOrderByInvitedAtAsc(rfq.getId()).stream()
                .map(RfqResponse.RfqVendorResponse::from)
                .toList();
        return RfqResponse.from(rfq, items, vendors);
    }

    private RfqResponse toHeaderResponse(Rfq rfq) {
        return RfqResponse.from(rfq, null, null);
    }
}
