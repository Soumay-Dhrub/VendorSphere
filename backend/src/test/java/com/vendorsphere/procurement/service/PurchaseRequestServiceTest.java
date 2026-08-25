package com.vendorsphere.procurement.service;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.service.AuditService;
import com.vendorsphere.auth.security.UserPrincipal;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.util.ReferenceNumberGenerator;
import com.vendorsphere.common.util.ReferencePrefix;
import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.service.NotificationService;
import com.vendorsphere.organization.entity.Department;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.organization.repository.DepartmentRepository;
import com.vendorsphere.procurement.Priority;
import com.vendorsphere.procurement.PurchaseRequestStatus;
import com.vendorsphere.procurement.dto.PurchaseRequestHeaderRequest;
import com.vendorsphere.procurement.dto.PurchaseRequestItemRequest;
import com.vendorsphere.procurement.dto.PurchaseRequestResponse;
import com.vendorsphere.procurement.dto.PurchaseRequestReviewRequest;
import com.vendorsphere.procurement.dto.PurchaseRequestStateSnapshot;
import com.vendorsphere.procurement.entity.PurchaseRequest;
import com.vendorsphere.procurement.repository.PurchaseRequestItemRepository;
import com.vendorsphere.procurement.repository.PurchaseRequestRepository;
import com.vendorsphere.user.RoleName;
import com.vendorsphere.user.entity.Role;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PurchaseRequestServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-14T09:15:30Z");

    private final PurchaseRequestRepository purchaseRequestRepository =
            mock(PurchaseRequestRepository.class);
    private final PurchaseRequestItemRepository itemRepository =
            mock(PurchaseRequestItemRepository.class);
    private final DepartmentRepository departmentRepository = mock(DepartmentRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ReferenceNumberGenerator referenceNumberGenerator =
            mock(ReferenceNumberGenerator.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AuditService auditService = mock(AuditService.class);

    private final PurchaseRequestAccess access = new PurchaseRequestAccess();

    private final PurchaseRequestService service = new PurchaseRequestService(
            purchaseRequestRepository,
            itemRepository,
            departmentRepository,
            userRepository,
            referenceNumberGenerator,
            notificationService,
            auditService,
            access,
            Clock.fixed(NOW, ZoneOffset.UTC));

    private final UUID organizationId = UUID.randomUUID();
    private final UUID requesterId = UUID.randomUUID();
    private Organization organization;
    private Department department;
    private User requesterUser;
    private UUID reviewerId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(organizationId);

        department = new Department();
        department.setId(UUID.randomUUID());
        department.setOrganization(organization);
        department.setName("Engineering");
        when(departmentRepository.findByIdAndOrganizationId(department.getId(), organizationId))
                .thenReturn(Optional.of(department));

        requesterUser = user("requester@demo-corp.com", RoleName.REQUESTER);
        requesterUser.setId(requesterId);

        // The authenticated reviewer/actor: approvals record this account.
        User managerUser = user("manager@demo-corp.com", RoleName.PROCUREMENT_MANAGER);
        managerUser.setId(UUID.randomUUID());
        reviewerId = managerUser.getId();
        actorId = reviewerId;
        when(userRepository.getReferenceById(any(UUID.class))).thenAnswer(call -> {
            UUID id = call.getArgument(0, UUID.class);
            if (id.equals(requesterId)) {
                return requesterUser;
            }
            User reference = new User();
            reference.setId(id);
            reference.setOrganization(organization);
            reference.setEmail("ref-" + id + "@demo-corp.com");
            return reference;
        });

        UserPrincipal principal = new UserPrincipal(managerUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        // A real persist assigns the identifier; echo that so detail assembly can key on it.
        when(purchaseRequestRepository.save(any(PurchaseRequest.class)))
                .thenAnswer(call -> {
                    PurchaseRequest saved = call.getArgument(0, PurchaseRequest.class);
                    if (saved.getId() == null) {
                        saved.setId(UUID.randomUUID());
                    }
                    return saved;
                });
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ----- creation (Requirement 7.1, 7.2) -----

    @Test
    void createStartsAsADraftWithAGeneratedNumberAndTheActorAsRequester() {
        when(referenceNumberGenerator.allocate(organizationId, ReferencePrefix.PR))
                .thenReturn("PR-2026-007");

        PurchaseRequestResponse created = service.create(header());

        assertThat(created.status()).isEqualTo(PurchaseRequestStatus.DRAFT);
        assertThat(created.requestNumber()).isEqualTo("PR-2026-007");
        // The actor is the requester by construction (Requirement 7.1).
        assertThat(created.requesterId()).isEqualTo(actorId);
        assertThat(created.priority()).isEqualTo(Priority.MEDIUM);
        verify(purchaseRequestRepository).save(any(PurchaseRequest.class));
    }

    @Test
    void createAppliesTheMediumDefaultAndNormalizesTheBudget() {
        service.create(new PurchaseRequestHeaderRequest(
                "Laptops", department.getId(), null, null, null,
                new BigDecimal("1200000")));
        PurchaseRequest stored = storedCapture();
        assertThat(stored.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(stored.getEstimatedBudget())
                .isEqualByComparingTo(new BigDecimal("1200000.00"));
    }

    @Test
    void createHonorsAnExplicitPriority() {
        service.create(new PurchaseRequestHeaderRequest(
                "Laptops", department.getId(), "New hires", null, Priority.LOW, null));
        assertThat(storedCapture().getPriority()).isEqualTo(Priority.LOW);
    }

    @Test
    void createWithAForeignDepartmentIsNotFound() {
        assertThatThrownBy(() -> service.create(new PurchaseRequestHeaderRequest(
                "Laptops", UUID.randomUUID(), null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Department not found")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(purchaseRequestRepository, never()).save(any());
    }

    // ----- items (Requirements 7.3, 7.4, 7.5) -----

    @Test
    void addItemStoresQuantityAtScaleAndSortOrderAsTheItemCount() {
        PurchaseRequest draft = storedDraft();
        when(itemRepository.countByPurchaseRequestId(draft.getId())).thenReturn(2L);

        service.addItem(draft.getId(), new PurchaseRequestItemRequest(
                "Monitor", new BigDecimal("20.5"), "PCS", "27-inch IPS"));

        ArgumentCaptor<com.vendorsphere.procurement.entity.PurchaseRequestItem> stored =
                ArgumentCaptor.forClass(com.vendorsphere.procurement.entity.PurchaseRequestItem.class);
        verify(itemRepository).save(stored.capture());
        assertThat(stored.getValue().getQuantity())
                .isEqualByComparingTo(new BigDecimal("20.500"));
        assertThat(stored.getValue().getQuantity().scale()).isEqualTo(3);
        assertThat(stored.getValue().getSortOrder()).isEqualTo(2);
        assertThat(stored.getValue().getUnit()).isEqualTo("PCS");
    }

    @Test
    void addItemWithoutAUnitStoresUnit() {
        PurchaseRequest draft = storedDraft();
        when(itemRepository.countByPurchaseRequestId(draft.getId())).thenReturn(0L);

        service.addItem(draft.getId(),
                new PurchaseRequestItemRequest("Keyboard", BigDecimal.ONE, " ", null));

        ArgumentCaptor<com.vendorsphere.procurement.entity.PurchaseRequestItem> stored =
                ArgumentCaptor.forClass(com.vendorsphere.procurement.entity.PurchaseRequestItem.class);
        verify(itemRepository).save(stored.capture());
        assertThat(stored.getValue().getUnit()).isEqualTo("UNIT");
        assertThat(stored.getValue().getSortOrder()).isZero();
    }

    @Test
    void addItemWithANonPositiveQuantityIsRejected() {
        PurchaseRequest draft = storedDraft();

        assertThatThrownBy(() -> service.addItem(draft.getId(),
                new PurchaseRequestItemRequest("Keyboard", BigDecimal.ZERO, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Quantity must be greater than zero")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(itemRepository, never()).save(any());
    }

    @Test
    void itemsAreLockedAfterSubmission() {
        PurchaseRequest submitted = storedInStatus(PurchaseRequestStatus.SUBMITTED);

        assertThatThrownBy(() -> service.addItem(submitted.getId(),
                new PurchaseRequestItemRequest("Keyboard", BigDecimal.ONE, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Purchase request items are locked after submission")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(itemRepository, never()).save(any());
    }

    @Test
    void headerEditsAreLockedAfterSubmissionToo() {
        PurchaseRequest submitted = storedInStatus(PurchaseRequestStatus.SUBMITTED);

        assertThatThrownBy(() -> service.update(submitted.getId(), header()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Purchase request items are locked after submission")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    // ----- submission and review (Requirements 7.7, 8.3 through 8.7) -----

    @Test
    void submittingARequestWithoutItemsIsRejected() {
        PurchaseRequest draft = storedDraft();
        when(itemRepository.countByPurchaseRequestId(draft.getId())).thenReturn(0L);

        assertThatThrownBy(() -> service.submit(draft.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Purchase request requires at least one item")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(draft.getStatus()).isEqualTo(PurchaseRequestStatus.DRAFT);
        verifyNoInteractions(auditService);
        verifyNoInteractions(notificationService);
    }

    @Test
    void submitNotifiesManagersAndRecordsTheTrailEntry() {
        PurchaseRequest draft = storedDraft();
        when(itemRepository.countByPurchaseRequestId(draft.getId())).thenReturn(1L);

        service.submit(draft.getId());

        assertThat(draft.getStatus()).isEqualTo(PurchaseRequestStatus.SUBMITTED);
        verify(notificationService).createForRole(
                eq(organizationId), eq(RoleName.PROCUREMENT_MANAGER),
                eq(NotificationEvent.PURCHASE_REQUEST_SUBMITTED),
                eq("PurchaseRequest"), eq(draft.getId()), any(), any());
        ArgumentCaptor<Object> previous = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> current = ArgumentCaptor.forClass(Object.class);
        verify(auditService).record(eq(AuditAction.PURCHASE_REQUEST_SUBMITTED),
                eq("PurchaseRequest"), eq(draft.getId()),
                previous.capture(), current.capture());
        assertThat(previous.getValue()).isEqualTo(new PurchaseRequestStateSnapshot(
                draft.getId(), PurchaseRequestStatus.DRAFT, null));
        assertThat(current.getValue()).isEqualTo(new PurchaseRequestStateSnapshot(
                draft.getId(), PurchaseRequestStatus.SUBMITTED, null));
    }

    @Test
    void approvingAnAlreadyApprovedRequestIsRejectedByTheMachine() {
        PurchaseRequest approved = storedInStatus(PurchaseRequestStatus.APPROVED);

        assertThatThrownBy(() -> service.approve(approved.getId(),
                new PurchaseRequestReviewRequest("Fine")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot transition from APPROVED to APPROVED")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void approvalRecordsTheReviewerFieldsAndNotes() {
        PurchaseRequest underReview = storedInStatus(PurchaseRequestStatus.UNDER_REVIEW);

        PurchaseRequestResponse approved = service.approve(underReview.getId(),
                new PurchaseRequestReviewRequest("Within budget"));

        assertThat(underReview.getStatus()).isEqualTo(PurchaseRequestStatus.APPROVED);
        assertThat(underReview.getReviewedAt()).isEqualTo(NOW);
        assertThat(underReview.getReviewNotes()).isEqualTo("Within budget");
        assertThat(approved.reviewedById()).isEqualTo(reviewerId);
        verify(auditService).record(eq(AuditAction.PURCHASE_REQUEST_APPROVED),
                eq("PurchaseRequest"), eq(underReview.getId()), any(), any());
    }

    @Test
    void approvalFromSubmittedPassesThroughUnderReview() {
        PurchaseRequest submitted = storedInStatus(PurchaseRequestStatus.SUBMITTED);

        service.approve(submitted.getId(), new PurchaseRequestReviewRequest(null));

        assertThat(submitted.getStatus()).isEqualTo(PurchaseRequestStatus.APPROVED);
    }

    @Test
    void rejectionWithoutAReasonIsRejected() {
        PurchaseRequest underReview = storedInStatus(PurchaseRequestStatus.UNDER_REVIEW);

        assertThatThrownBy(() -> service.reject(underReview.getId(),
                new PurchaseRequestReviewRequest("   ")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Rejection reason is required")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(underReview.getStatus()).isEqualTo(PurchaseRequestStatus.UNDER_REVIEW);
        verify(purchaseRequestRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void rejectionRecordsTheReasonAndNotifiesTheRequester() {
        PurchaseRequest submitted = storedInStatus(PurchaseRequestStatus.SUBMITTED);

        service.reject(submitted.getId(), new PurchaseRequestReviewRequest("Budget frozen"));

        assertThat(submitted.getStatus()).isEqualTo(PurchaseRequestStatus.REJECTED);
        assertThat(submitted.getReviewNotes()).isEqualTo("Budget frozen");
        verify(notificationService).createOnce(
                eq(requesterId), eq(NotificationEvent.PURCHASE_REQUEST_REJECTED),
                eq("PurchaseRequest"), eq(submitted.getId()), any(), any());
        verify(auditService).record(eq(AuditAction.PURCHASE_REQUEST_REJECTED),
                eq("PurchaseRequest"), eq(submitted.getId()), any(), any());
    }

    // ----- visibility (Requirements 8.9, 30.6, 30.10) -----

    @Test
    void aCrossTenantRequestIsNotFound() {
        UUID foreignId = UUID.randomUUID();
        when(purchaseRequestRepository.findByIdAndOrganizationId(foreignId, organizationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(foreignId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Purchase request not found");
    }

    // ----- fixtures -----

    private PurchaseRequestHeaderRequest header() {
        return new PurchaseRequestHeaderRequest(
                "20 development laptops", department.getId(),
                "New engineering hires", null, Priority.MEDIUM,
                new BigDecimal("1200000"));
    }

    private PurchaseRequest storedCapture() {
        ArgumentCaptor<PurchaseRequest> stored = ArgumentCaptor.forClass(PurchaseRequest.class);
        verify(purchaseRequestRepository).save(stored.capture());
        return stored.getValue();
    }

    private PurchaseRequest storedDraft() {
        return storedInStatus(PurchaseRequestStatus.DRAFT);
    }

    private PurchaseRequest storedInStatus(PurchaseRequestStatus status) {
        PurchaseRequest request = new PurchaseRequest();
        request.setId(UUID.randomUUID());
        request.setOrganization(organization);
        request.setRequester(requesterUser);
        request.setDepartment(department);
        request.setRequestNumber("PR-2026-042");
        request.setTitle("20 development laptops");
        request.setStatus(status);
        when(purchaseRequestRepository.findByIdAndOrganizationId(request.getId(), organizationId))
                .thenReturn(Optional.of(request));
        return request;
    }

    private User user(String email, String... roleNames) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setOrganization(organization);
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setRoles(java.util.Arrays.stream(roleNames)
                .map(roleName -> {
                    Role role = new Role();
                    role.setName(roleName);
                    return role;
                })
                .collect(Collectors.toSet()));
        return user;
    }
}
