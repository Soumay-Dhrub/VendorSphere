package com.vendorsphere.procurement.service;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.service.AuditService;
import com.vendorsphere.common.dto.PageResponse;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.common.util.Money;
import com.vendorsphere.common.util.PageSupport;
import com.vendorsphere.common.util.ReferenceNumberGenerator;
import com.vendorsphere.common.util.ReferencePrefix;
import com.vendorsphere.common.util.SortWhitelist;
import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.service.NotificationService;
import com.vendorsphere.organization.entity.Department;
import com.vendorsphere.organization.repository.DepartmentRepository;
import com.vendorsphere.procurement.PurchaseRequestStatus;
import com.vendorsphere.procurement.PurchaseRequestStatusTransitions;
import com.vendorsphere.procurement.dto.PurchaseRequestHeaderRequest;
import com.vendorsphere.procurement.dto.PurchaseRequestItemRequest;
import com.vendorsphere.procurement.dto.PurchaseRequestResponse;
import com.vendorsphere.procurement.dto.PurchaseRequestReviewRequest;
import com.vendorsphere.procurement.dto.PurchaseRequestSearchCriteria;
import com.vendorsphere.procurement.dto.PurchaseRequestStateSnapshot;
import com.vendorsphere.procurement.entity.PurchaseRequest;
import com.vendorsphere.procurement.entity.PurchaseRequestItem;
import com.vendorsphere.procurement.repository.PurchaseRequestItemRepository;
import com.vendorsphere.procurement.repository.PurchaseRequestRepository;
import com.vendorsphere.procurement.repository.PurchaseRequestSpecifications;
import com.vendorsphere.user.RoleName;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PurchaseRequestService {

    /** Pinned by Requirement 7.5. */
    static final String QUANTITY_MESSAGE = "Quantity must be greater than zero";

    /** Pinned by Requirement 7.7. */
    static final String EMPTY_SUBMISSION_MESSAGE = "Purchase request requires at least one item";

    /** Pinned by Requirement 8.3. */
    static final String ITEMS_LOCKED_MESSAGE = "Purchase request items are locked after submission";

    /** Pinned by Requirement 8.6. */
    static final String REJECTION_REASON_MESSAGE = "Rejection reason is required";

    static final String NOT_FOUND_MESSAGE = "Purchase request not found";
    static final String DEPARTMENT_NOT_FOUND_MESSAGE = "Department not found";
    static final String ITEM_NOT_FOUND_MESSAGE = "Purchase request item not found";

    /** Sortable fields of the listing; the controller defaults to createdAt descending. */
    public static final SortWhitelist SORTABLE =
            SortWhitelist.of("createdAt", "requiredDate", "priority", "status");

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestItemRepository purchaseRequestItemRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final ReferenceNumberGenerator referenceNumberGenerator;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final PurchaseRequestAccess access;
    private final Clock clock;

    public PurchaseRequestService(
            PurchaseRequestRepository purchaseRequestRepository,
            PurchaseRequestItemRepository purchaseRequestItemRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            ReferenceNumberGenerator referenceNumberGenerator,
            NotificationService notificationService,
            AuditService auditService,
            PurchaseRequestAccess access,
            Clock clock
    ) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.purchaseRequestItemRepository = purchaseRequestItemRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.referenceNumberGenerator = referenceNumberGenerator;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.access = access;
        this.clock = clock;
    }

    /**
     * Creates a DRAFT request with a generated {@code PR} number, the actor as requester and the
     * actor's organization (Requirement 7.1), defaulting the priority to MEDIUM when absent
     * (Requirement 7.2).
     *
     * <p>The request number is allocated on this transaction, so it is consumed exactly when the row
     * commits and released when it rolls back (Requirement 1.5). The organization is taken from the
     * department after the department has been resolved within the caller's tenant, so a request can
     * never be filed under a foreign department or tenant.
     */
    @Transactional
    public PurchaseRequestResponse create(PurchaseRequestHeaderRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Department department = resolveDepartment(request.departmentId(), organizationId);

        PurchaseRequest purchaseRequest = new PurchaseRequest();
        purchaseRequest.setOrganization(department.getOrganization());
        purchaseRequest.setRequester(
                userRepository.getReferenceById(SecurityUtils.getCurrentUserId()));
        purchaseRequest.setRequestNumber(
                referenceNumberGenerator.allocate(organizationId, ReferencePrefix.PR));
        applyHeader(purchaseRequest, request, organizationId);

        PurchaseRequest saved = purchaseRequestRepository.save(purchaseRequest);
        return toDetailResponse(saved);
    }

    /**
     * Applies new header values to a DRAFT request. Authoring ends at submission, so any later state
     * is answered by {@link #findVisibleDraft} with the same pinned lock the items sit under: a
     * requester must not quietly change what procurement is already quoting.
     */
    @Transactional
    public PurchaseRequestResponse update(
            UUID purchaseRequestId, PurchaseRequestHeaderRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PurchaseRequest purchaseRequest = findVisibleDraft(purchaseRequestId, organizationId);

        applyHeader(purchaseRequest, request, organizationId);
        return toDetailResponse(purchaseRequestRepository.save(purchaseRequest));
    }

    private void applyHeader(
            PurchaseRequest purchaseRequest,
            PurchaseRequestHeaderRequest request,
            UUID organizationId) {
        purchaseRequest.setTitle(request.title());
        purchaseRequest.setBusinessJustification(request.businessJustification());
        purchaseRequest.setRequiredDate(request.requiredDate());
        // Requirement 7.2: MEDIUM when absent.
        purchaseRequest.setPriority(request.priority() == null
                ? com.vendorsphere.procurement.Priority.MEDIUM
                : request.priority());
        purchaseRequest.setEstimatedBudget(
                request.estimatedBudget() == null
                        ? null
                        : Money.money(request.estimatedBudget()));
        if (purchaseRequest.getDepartment() == null
                || !purchaseRequest.getDepartment().getId().equals(request.departmentId())) {
            purchaseRequest.setDepartment(resolveDepartment(request.departmentId(), organizationId));
        }
    }

    /** Resolves the department within the caller's organization; a foreign id misses as 404. */
    private Department resolveDepartment(UUID departmentId, UUID organizationId) {
        return departmentRepository.findByIdAndOrganizationId(departmentId, organizationId)
                .orElseThrow(() -> new BusinessException(
                        DEPARTMENT_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
    }

    /**
     * Adds one line item to a DRAFT request (Requirement 7.4).
     *
     * <p>The quantity is normalized to quantity scale before storage and must be strictly positive
     * (Requirement 7.5). The sort order is the current item count of the request, so items keep
     * authoring order without a client-supplied sequence that could collide or be forged.
     */
    @Transactional
    public PurchaseRequestResponse addItem(
            UUID purchaseRequestId, PurchaseRequestItemRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PurchaseRequest purchaseRequest = findVisibleDraft(purchaseRequestId, organizationId);

        PurchaseRequestItem item = new PurchaseRequestItem();
        applyItem(item, request);
        item.setPurchaseRequest(purchaseRequest);
        item.setSortOrder((int) purchaseRequestItemRepository.countByPurchaseRequestId(purchaseRequest.getId()));

        purchaseRequestItemRepository.save(item);
        return toDetailResponse(purchaseRequest);
    }

    /**
     * Updates one line item of a DRAFT request. The sort order is authoring state and survives an
     * edit: re-sequencing lines is not part of the requirement, and keeping it means a client cannot
     * reorder another requester's line items by rewriting them.
     */
    @Transactional
    public PurchaseRequestResponse updateItem(
            UUID purchaseRequestId, UUID itemId, PurchaseRequestItemRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PurchaseRequest purchaseRequest = findVisibleDraft(purchaseRequestId, organizationId);
        PurchaseRequestItem item = findItem(purchaseRequestId, itemId, organizationId);

        applyItem(item, request);
        return toDetailResponse(purchaseRequest);
    }

    /** Removes one line item from a DRAFT request. Remaining items keep their sort orders. */
    @Transactional
    public PurchaseRequestResponse removeItem(UUID purchaseRequestId, UUID itemId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PurchaseRequest purchaseRequest = findVisibleDraft(purchaseRequestId, organizationId);
        PurchaseRequestItem item = findItem(purchaseRequestId, itemId, organizationId);

        purchaseRequestItemRepository.delete(item);
        return toDetailResponse(purchaseRequest);
    }

    private void applyItem(PurchaseRequestItem item, PurchaseRequestItemRequest request) {
        if (request.quantity() == null
                || request.quantity().signum() <= 0) {
            throw new BusinessException(QUANTITY_MESSAGE, HttpStatus.BAD_REQUEST);
        }
        item.setItemName(request.itemName().trim());
        item.setQuantity(Money.quantity(request.quantity()));
        // An absent or blank unit stores as the column default UNIT.
        item.setUnit(request.unit() == null || request.unit().isBlank()
                ? "UNIT"
                : request.unit().trim());
        item.setSpecification(request.specification());
    }

    /**
     * Loads an item through its parent request and organization, so both parts of the nested path
     * must agree before an item is touched - an item id hanging off a different request is reported
     * as {@code Purchase request item not found} for the same reason vendor contact ids are.
     */
    private PurchaseRequestItem findItem(
            UUID purchaseRequestId, UUID itemId, UUID organizationId) {
        PurchaseRequestItem item = purchaseRequestItemRepository
                .findByIdAndPurchaseRequestOrganizationId(itemId, organizationId)
                .orElseThrow(() -> new BusinessException(
                        ITEM_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        if (!item.getPurchaseRequest().getId().equals(purchaseRequestId)) {
            throw new BusinessException(ITEM_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND);
        }
        return item;
    }

    /**
     * Submits a DRAFT request for review (Requirement 8.1).
     *
     * <p>The empty-item guard runs before the transition is asserted, so a request with no lines is
     * answered with the 400 of Requirement 7.7 whether or not the state change would have been legal;
     * an empty request is invalid to submit from any state that could hold items.
     *
     * <p>On success every PROCUREMENT_MANAGER of the organization is notified (Requirement 8.4) and a
     * {@code PURCHASE_REQUEST_SUBMITTED} trail entry records previous and current state
     * (Requirement 29.2). Items are locked from here on by every authoring path's DRAFT check.
     */
    @Transactional
    public PurchaseRequestResponse submit(UUID purchaseRequestId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PurchaseRequest purchaseRequest = findVisible(purchaseRequestId, organizationId);

        if (purchaseRequestItemRepository.countByPurchaseRequestId(purchaseRequest.getId()) < 1) {
            throw new BusinessException(EMPTY_SUBMISSION_MESSAGE, HttpStatus.BAD_REQUEST);
        }
        PurchaseRequestStateSnapshot previous =
                PurchaseRequestStateSnapshot.from(purchaseRequest);

        purchaseRequest.setStatus(transition(purchaseRequest.getStatus(),
                PurchaseRequestStatus.SUBMITTED));
        PurchaseRequest saved = purchaseRequestRepository.save(purchaseRequest);

        notificationService.createForRole(organizationId, RoleName.PROCUREMENT_MANAGER,
                NotificationEvent.PURCHASE_REQUEST_SUBMITTED,
                "PurchaseRequest", saved.getId(),
                "Purchase request submitted",
                saved.getRequestNumber() + " - " + saved.getTitle()
                        + " awaits procurement review.");
        auditService.record(AuditAction.PURCHASE_REQUEST_SUBMITTED, "PurchaseRequest",
                saved.getId(), previous, PurchaseRequestStateSnapshot.from(saved));
        return toDetailResponse(saved);
    }

    /**
     * Approves a submitted or under-review request (Requirement 8.5), recording the actor as
     * reviewer, the decision instant as review timestamp and the supplied comments as review notes.
     *
     * <h4>Why SUBMITTED requests pass through UNDER_REVIEW</h4>
     *
     * <p>The transition table of Requirement 8.1 routes decisions through UNDER_REVIEW, but the API
     * surface declares no separate "start review" endpoint, so a decision made on a freshly submitted
     * request walks its first step internally: {@code SUBMITTED&rarr;UNDER_REVIEW} is asserted, then
     * {@code UNDER_REVIEW&rarr;APPROVED}. Every pair exercised is one of the permitted pairs, and a
     * request in any other state still fails the machine with its own 409 wording.
     */
    @Transactional
    public PurchaseRequestResponse approve(
            UUID purchaseRequestId, PurchaseRequestReviewRequest request) {
        return decide(purchaseRequestId, request, PurchaseRequestStatus.APPROVED);
    }

    /**
     * Rejects a submitted or under-review request with a mandatory reason (Requirements 8.6, 8.7):
     * the reason becomes the review notes and the requester is notified. A blank reason counts as no
     * reason, so whitespace cannot satisfy Requirement 8.6.
     */
    @Transactional
    public PurchaseRequestResponse reject(
            UUID purchaseRequestId, PurchaseRequestReviewRequest request) {
        String reason = normalized(request == null ? null : request.comments());
        if (reason == null) {
            throw new BusinessException(REJECTION_REASON_MESSAGE, HttpStatus.BAD_REQUEST);
        }
        return decide(purchaseRequestId, request, PurchaseRequestStatus.REJECTED, reason);
    }

    private PurchaseRequestResponse decide(
            UUID purchaseRequestId,
            PurchaseRequestReviewRequest request,
            PurchaseRequestStatus target) {
        return decide(purchaseRequestId, request, target,
                normalized(request == null ? null : request.comments()));
    }

    private PurchaseRequestResponse decide(
            UUID purchaseRequestId,
            PurchaseRequestReviewRequest request,
            PurchaseRequestStatus target,
            String notes) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PurchaseRequest purchaseRequest = findVisible(purchaseRequestId, organizationId);
        PurchaseRequestStateSnapshot previous = PurchaseRequestStateSnapshot.from(purchaseRequest);

        // Walk SUBMITTED through UNDER_REVIEW first; see the approve() javadoc.
        PurchaseRequestStatus current = purchaseRequest.getStatus();
        if (current == PurchaseRequestStatus.SUBMITTED) {
            current = transition(current, PurchaseRequestStatus.UNDER_REVIEW);
        }
        purchaseRequest.setStatus(transition(current, target));
        purchaseRequest.setReviewedBy(
                userRepository.getReferenceById(SecurityUtils.getCurrentUserId()));
        purchaseRequest.setReviewedAt(clock.instant());
        purchaseRequest.setReviewNotes(notes);

        PurchaseRequest saved = purchaseRequestRepository.save(purchaseRequest);

        if (target == PurchaseRequestStatus.REJECTED) {
            notificationService.createOnce(saved.getRequester().getId(),
                    NotificationEvent.PURCHASE_REQUEST_REJECTED,
                    "PurchaseRequest", saved.getId(),
                    "Purchase request rejected",
                    saved.getRequestNumber() + " was rejected: " + saved.getReviewNotes());
        }
        AuditAction action = target == PurchaseRequestStatus.APPROVED
                ? AuditAction.PURCHASE_REQUEST_APPROVED
                : AuditAction.PURCHASE_REQUEST_REJECTED;
        auditService.record(action, "PurchaseRequest", saved.getId(), previous,
                PurchaseRequestStateSnapshot.from(saved));
        return toDetailResponse(saved);
    }

    /** Asserts one machine step and answers the target, keeping call sites flat. */
    private PurchaseRequestStatus transition(PurchaseRequestStatus from, PurchaseRequestStatus to) {
        PurchaseRequestStatusTransitions.MACHINE.assertTransition(from, to);
        return to;
    }

    /**
     * Returns one request with its items, review data and derived RFQ identifiers
     * (Requirement 8.9). Requester-only callers are narrowed to their own requests by
     * {@link PurchaseRequestAccess}; any other identifier is 404.
     */
    @Transactional(readOnly = true)
    public PurchaseRequestResponse get(UUID purchaseRequestId) {
        PurchaseRequest purchaseRequest = findVisible(
                purchaseRequestId, SecurityUtils.getCurrentOrganizationId());
        return toDetailResponse(purchaseRequest);
    }

    /**
     * A page of the caller's organization's requests, narrowed by the optional filters.
     *
     * <p>Paging defaults, the size clamp and the sort allowlist belong to {@code PageSupport}, used
     * by the controller with {@link #SORTABLE}. A page costs five queries whatever its size - content,
     * count, items, RFQ identifiers, reviewer names - because all three projections are batched for
     * the whole page rather than read per row (Requirement 31.2).
     */
    @Transactional(readOnly = true)
    public PageResponse<PurchaseRequestResponse> search(
            PurchaseRequestSearchCriteria criteria, Pageable pageable) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID restrictedRequesterId = access.restrictedRequesterId();
        Page<PurchaseRequest> page = purchaseRequestRepository.findAll(
                com.vendorsphere.procurement.repository.PurchaseRequestSpecifications.search(
                        organizationId, criteria, restrictedRequesterId),
                pageable);

        List<PurchaseRequest> rows = page.getContent();
        List<UUID> ids = rows.stream().map(PurchaseRequest::getId).toList();
        Map<UUID, List<PurchaseRequestItem>> items =
                purchaseRequestItemRepository.itemsByPurchaseRequestId(ids);
        Map<UUID, List<UUID>> rfqIds = purchaseRequestRepository.rfqIdsByPurchaseRequestId(ids);
        Map<UUID, User> reviewers = reviewersOf(rows);

        return PageSupport.map(page, request -> PurchaseRequestResponse.from(
                request,
                items.getOrDefault(request.getId(), List.of()).stream()
                        .map(PurchaseRequestResponse.PurchaseRequestItemResponse::from)
                        .toList(),
                rfqIds.getOrDefault(request.getId(), List.of()),
                reviewerName(reviewers.get(
                        request.getReviewedBy() == null
                                ? null
                                : request.getReviewedBy().getId()))));
    }

    // ----- helpers -----

    /**
     * Loads a request within the caller's organization and applies the visibility rule. Every path in
     * this service enters through here, so no endpoint can bypass either tenant scope or the
     * requester-only narrowing (Requirements 30.10, 8.9).
     */
    private PurchaseRequest findVisible(UUID purchaseRequestId, UUID organizationId) {
        PurchaseRequest purchaseRequest = purchaseRequestRepository
                .findByIdAndOrganizationId(purchaseRequestId, organizationId)
                .orElseThrow(() -> new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        access.assertReadable(purchaseRequest);
        return purchaseRequest;
    }

    /** {@link #findVisible} plus the DRAFT-only authoring gate of Requirements 7.3 and 8.3. */
    private PurchaseRequest findVisibleDraft(UUID purchaseRequestId, UUID organizationId) {
        PurchaseRequest purchaseRequest = findVisible(purchaseRequestId, organizationId);
        if (purchaseRequest.getStatus() != PurchaseRequestStatus.DRAFT) {
            throw new BusinessException(ITEMS_LOCKED_MESSAGE, HttpStatus.CONFLICT);
        }
        return purchaseRequest;
    }

    /** Reviewers of a page resolved in one query; nulls are skipped rather than queried. */
    private Map<UUID, User> reviewersOf(List<PurchaseRequest> requests) {
        List<UUID> reviewerIds = requests.stream()
                .map(PurchaseRequest::getReviewedBy)
                .filter(java.util.Objects::nonNull)
                .map(User::getId)
                .distinct()
                .toList();
        if (reviewerIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(reviewerIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }

    /** Display name of a reviewer: given and family name joined, tolerating absent parts. */
    private String reviewerName(User reviewer) {
        if (reviewer == null) {
            return null;
        }
        String name = (reviewer.getFirstName() + " " + reviewer.getLastName()).trim();
        return name.isEmpty() ? reviewer.getEmail() : name;
    }

    /**
     * The detail projection of one request: items in authoring order, the identifiers of RFQs sourced
     * from it (native read over the V1 table; see {@code PurchaseRequestRepository}) and the
     * reviewer's display name when a decision has been recorded (Requirement 8.9).
     */
    private PurchaseRequestResponse toDetailResponse(PurchaseRequest purchaseRequest) {
        List<PurchaseRequestResponse.PurchaseRequestItemResponse> items = purchaseRequestItemRepository
                .findByPurchaseRequestIdOrderBySortOrderAscIdAsc(purchaseRequest.getId())
                .stream()
                .map(PurchaseRequestResponse.PurchaseRequestItemResponse::from)
                .toList();
        List<UUID> derivedRfqIds = purchaseRequestRepository
                .rfqIdsByPurchaseRequestId(List.of(purchaseRequest.getId()))
                .getOrDefault(purchaseRequest.getId(), List.of());
        User reviewedBy = purchaseRequest.getReviewedBy() == null
                ? null
                : userRepository.findById(purchaseRequest.getReviewedBy().getId()).orElse(null);

        return PurchaseRequestResponse.from(
                purchaseRequest, items, derivedRfqIds, reviewerName(reviewedBy));
    }

    private static String normalized(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
