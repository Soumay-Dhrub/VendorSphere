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

    static final String QUANTITY_MESSAGE = "Quantity must be greater than zero";

    static final String EMPTY_SUBMISSION_MESSAGE = "Purchase request requires at least one item";

    static final String ITEMS_LOCKED_MESSAGE = "Purchase request items are locked after submission";

    static final String REJECTION_REASON_MESSAGE = "Rejection reason is required";

    static final String NOT_FOUND_MESSAGE = "Purchase request not found";
    static final String DEPARTMENT_NOT_FOUND_MESSAGE = "Department not found";
    static final String ITEM_NOT_FOUND_MESSAGE = "Purchase request item not found";

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

    private Department resolveDepartment(UUID departmentId, UUID organizationId) {
        return departmentRepository.findByIdAndOrganizationId(departmentId, organizationId)
                .orElseThrow(() -> new BusinessException(
                        DEPARTMENT_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
    }

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

    @Transactional
    public PurchaseRequestResponse updateItem(
            UUID purchaseRequestId, UUID itemId, PurchaseRequestItemRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PurchaseRequest purchaseRequest = findVisibleDraft(purchaseRequestId, organizationId);
        PurchaseRequestItem item = findItem(purchaseRequestId, itemId, organizationId);

        applyItem(item, request);
        return toDetailResponse(purchaseRequest);
    }

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

    @Transactional
    public PurchaseRequestResponse approve(
            UUID purchaseRequestId, PurchaseRequestReviewRequest request) {
        return decide(purchaseRequestId, request, PurchaseRequestStatus.APPROVED);
    }

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

    private PurchaseRequestStatus transition(PurchaseRequestStatus from, PurchaseRequestStatus to) {
        PurchaseRequestStatusTransitions.MACHINE.assertTransition(from, to);
        return to;
    }

    @Transactional(readOnly = true)
    public PurchaseRequestResponse get(UUID purchaseRequestId) {
        PurchaseRequest purchaseRequest = findVisible(
                purchaseRequestId, SecurityUtils.getCurrentOrganizationId());
        return toDetailResponse(purchaseRequest);
    }

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

    private PurchaseRequest findVisible(UUID purchaseRequestId, UUID organizationId) {
        PurchaseRequest purchaseRequest = purchaseRequestRepository
                .findByIdAndOrganizationId(purchaseRequestId, organizationId)
                .orElseThrow(() -> new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        access.assertReadable(purchaseRequest);
        return purchaseRequest;
    }

    private PurchaseRequest findVisibleDraft(UUID purchaseRequestId, UUID organizationId) {
        PurchaseRequest purchaseRequest = findVisible(purchaseRequestId, organizationId);
        if (purchaseRequest.getStatus() != PurchaseRequestStatus.DRAFT) {
            throw new BusinessException(ITEMS_LOCKED_MESSAGE, HttpStatus.CONFLICT);
        }
        return purchaseRequest;
    }

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

    private String reviewerName(User reviewer) {
        if (reviewer == null) {
            return null;
        }
        String name = (reviewer.getFirstName() + " " + reviewer.getLastName()).trim();
        return name.isEmpty() ? reviewer.getEmail() : name;
    }

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
