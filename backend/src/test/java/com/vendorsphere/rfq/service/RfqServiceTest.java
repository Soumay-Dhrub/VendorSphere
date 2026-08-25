package com.vendorsphere.rfq.service;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.service.AuditService;
import com.vendorsphere.auth.security.UserPrincipal;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.util.ReferenceNumberGenerator;
import com.vendorsphere.common.util.ReferencePrefix;
import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.service.NotificationService;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.procurement.PurchaseRequestStatus;
import com.vendorsphere.procurement.entity.PurchaseRequest;
import com.vendorsphere.procurement.entity.PurchaseRequestItem;
import com.vendorsphere.procurement.repository.PurchaseRequestItemRepository;
import com.vendorsphere.procurement.repository.PurchaseRequestRepository;
import com.vendorsphere.rfq.RfqStatus;
import com.vendorsphere.rfq.dto.RfqCancelRequest;
import com.vendorsphere.rfq.dto.RfqCreateRequest;
import com.vendorsphere.rfq.dto.RfqResponse;
import com.vendorsphere.rfq.entity.Rfq;
import com.vendorsphere.rfq.repository.RfqItemRepository;
import com.vendorsphere.rfq.repository.RfqRepository;
import com.vendorsphere.rfq.repository.RfqVendorRepository;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.user.repository.UserRepository;
import com.vendorsphere.vendor.entity.Vendor;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RfqServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-14T09:15:30Z");

    private final RfqRepository rfqRepository = mock(RfqRepository.class);
    private final RfqItemRepository rfqItemRepository = mock(RfqItemRepository.class);
    private final RfqVendorRepository rfqVendorRepository = mock(RfqVendorRepository.class);
    private final PurchaseRequestRepository purchaseRequestRepository =
            mock(PurchaseRequestRepository.class);
    private final PurchaseRequestItemRepository purchaseRequestItemRepository =
            mock(PurchaseRequestItemRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ReferenceNumberGenerator referenceNumberGenerator =
            mock(ReferenceNumberGenerator.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AuditService auditService = mock(AuditService.class);

    private final RfqService service = new RfqService(
            rfqRepository, rfqItemRepository, rfqVendorRepository,
            purchaseRequestRepository, purchaseRequestItemRepository,
            userRepository, referenceNumberGenerator, notificationService, auditService,
            Clock.fixed(NOW, ZoneOffset.UTC));

    private final UUID organizationId = UUID.randomUUID();
    private Organization organization;
    private PurchaseRequest sourceRequest;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(organizationId);

        User creator = new User();
        creator.setId(UUID.randomUUID());
        creator.setOrganization(organization);
        creator.setEmail("officer@demo-corp.com");
        creator.setPasswordHash("hash");
        when(userRepository.getReferenceById(any(UUID.class))).thenReturn(creator);

        var principal = new UserPrincipal(creator);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(referenceNumberGenerator.allocate(organizationId, ReferencePrefix.RFQ))
                .thenReturn("RFQ-2026-0042");
        when(rfqRepository.save(any(Rfq.class))).thenAnswer(call -> {
            Rfq saved = call.getArgument(0, Rfq.class);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });
        when(purchaseRequestRepository.save(any(PurchaseRequest.class)))
                .thenAnswer(call -> call.getArgument(0));
        when(purchaseRequestItemRepository.findByPurchaseRequestIdOrderBySortOrderAscIdAsc(any()))
                .thenReturn(List.of());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ----- creation (Requirements 9.1 through 9.4) -----

    @Test
    void createFromAnUnapprovedRequestNamesTheStatusInA409() {
        storedSource(PurchaseRequestStatus.DRAFT);

        assertThatThrownBy(() -> service.create(createRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot create an RFQ from a DRAFT purchase request")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(rfqRepository, never()).save(any());
    }

    @Test
    void createWithClosingNotAfterOpeningIsRejected() {
        storedSource(PurchaseRequestStatus.APPROVED);
        Instant opening = Instant.parse("2026-04-01T00:00:00Z");

        assertThatThrownBy(() -> service.create(new RfqCreateRequest(
                sourceRequest.getId(), "Laptops", null, opening, opening, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Closing date must be after opening date")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThatThrownBy(() -> service.create(new RfqCreateRequest(
                sourceRequest.getId(), "Laptops", null, opening, opening.minusSeconds(1),
                null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Closing date must be after opening date");
    }

    @Test
    void createCopiesItemsWithSourceIdsAndStartsProcurement() {
        PurchaseRequest source = storedSource(PurchaseRequestStatus.APPROVED);
        PurchaseRequestItem laptop = sourceItem(source, "Laptop", 0);
        PurchaseRequestItem monitor = sourceItem(source, "Monitor", 1);
        when(purchaseRequestItemRepository.findByPurchaseRequestIdOrderBySortOrderAscIdAsc(
                source.getId())).thenReturn(List.of(laptop, monitor));

        RfqResponse created = service.create(createRequest());

        assertThat(created.status()).isEqualTo(RfqStatus.DRAFT);
        assertThat(created.rfqNumber()).isEqualTo("RFQ-2026-0042");
        ArgumentCaptor<com.vendorsphere.rfq.entity.RfqItem> items =
                ArgumentCaptor.forClass(com.vendorsphere.rfq.entity.RfqItem.class);
        verify(rfqItemRepository, org.mockito.Mockito.times(2)).save(items.capture());
        assertThat(items.getAllValues()).extracting(
                        com.vendorsphere.rfq.entity.RfqItem::getSourceItemId,
                        com.vendorsphere.rfq.entity.RfqItem::getItemName,
                        com.vendorsphere.rfq.entity.RfqItem::getSortOrder)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(laptop.getId(), "Laptop", 0),
                        org.assertj.core.groups.Tuple.tuple(monitor.getId(), "Monitor", 1));
        assertThat(source.getStatus()).isEqualTo(PurchaseRequestStatus.PROCUREMENT_STARTED);
        verify(auditService).record(eq(AuditAction.RFQ_CREATED), eq("Rfq"),
                eq(created.id()), any(), any());
    }

    @Test
    void createFromAStartedRequestLeavesItsStatusAlone() {
        PurchaseRequest source = storedSource(PurchaseRequestStatus.PROCUREMENT_STARTED);

        service.create(createRequest());

        assertThat(source.getStatus()).isEqualTo(PurchaseRequestStatus.PROCUREMENT_STARTED);
        verify(purchaseRequestRepository, never()).save(any());
    }

    // ----- editing, opening and cancellation (Requirements 9.5, 10.6, 11) -----

    @Test
    void editingANonDraftRfqIsRejected() {
        Rfq openRfq = storedRfq(RfqStatus.OPEN);

        assertThatThrownBy(() -> service.update(openRfq.getId(), updateRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("RFQ can only be changed while in DRAFT")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void openingWithoutInvitationsIsRejected() {
        Rfq draft = storedRfq(RfqStatus.DRAFT);
        when(rfqVendorRepository.countByRfqId(draft.getId())).thenReturn(0L);

        assertThatThrownBy(() -> service.open(draft.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("RFQ requires at least one invited vendor")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(draft.getStatus()).isEqualTo(RfqStatus.DRAFT);
    }

    @Test
    void cancellingWithoutAReasonIsRejected() {
        Rfq openRfq = storedRfq(RfqStatus.OPEN);

        assertThatThrownBy(() -> service.cancel(openRfq.getId(),
                new RfqCancelRequest("   ")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cancellation reason is required")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(openRfq.getStatus()).isEqualTo(RfqStatus.OPEN);
    }

    @Test
    void cancellingAnAwardedRfqIsRejected() {
        Rfq awarded = storedRfq(RfqStatus.AWARDED);

        assertThatThrownBy(() -> service.cancel(awarded.getId(),
                new RfqCancelRequest("Supplier walked away")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Awarded RFQ cannot be cancelled")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(awarded.getStatus()).isEqualTo(RfqStatus.AWARDED);
        verify(rfqRepository, never()).rejectInFlightQuotations(awarded.getId());
    }

    @Test
    void cancellingRejectsInFlightQuotationsAndNotifiesInvitedVendors() {
        Rfq openRfq = storedRfq(RfqStatus.OPEN);
        UUID vendorA = UUID.randomUUID();
        UUID vendorB = UUID.randomUUID();
        when(rfqVendorRepository.findByRfqIdOrderByInvitedAtAsc(openRfq.getId()))
                .thenReturn(List.of(invitation(vendorA), invitation(vendorB)));
        when(rfqRepository.rejectInFlightQuotations(openRfq.getId())).thenReturn(2);

        RfqResponse cancelled = service.cancel(openRfq.getId(),
                new RfqCancelRequest("Requirement withdrawn"));

        assertThat(cancelled.status()).isEqualTo(RfqStatus.CANCELLED);
        assertThat(cancelled.cancellationReason()).isEqualTo("Requirement withdrawn");
        verify(rfqRepository).rejectInFlightQuotations(openRfq.getId());
        for (UUID vendorId : List.of(vendorA, vendorB)) {
            verify(notificationService).createForVendorUsers(
                    eq(vendorId), eq(NotificationEvent.RFQ_CANCELLED),
                    eq("Rfq"), eq(openRfq.getId()), any(), any());
        }
    }

    @Test
    void operationsOnAForeignRfqAreNotFound() {
        UUID foreignId = UUID.randomUUID();
        when(rfqRepository.findByIdAndOrganizationId(foreignId, organizationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(foreignId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("RFQ not found");
    }

    // ----- fixtures -----

    private RfqCreateRequest createRequest() {
        return new RfqCreateRequest(sourceRequest.getId(), "20 development laptops",
                "Engineering refresh",
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-04-15T00:00:00Z"), null, "Pune HQ", null);
    }

    private com.vendorsphere.rfq.dto.RfqUpdateRequest updateRequest() {
        return new com.vendorsphere.rfq.dto.RfqUpdateRequest(
                "Renamed RFQ", null,
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-04-20T00:00:00Z"), null, null, null);
    }

    private PurchaseRequest storedSource(PurchaseRequestStatus status) {
        sourceRequest = new PurchaseRequest();
        sourceRequest.setId(UUID.randomUUID());
        sourceRequest.setOrganization(organization);
        sourceRequest.setRequestNumber("PR-2026-007");
        sourceRequest.setTitle("20 development laptops");
        sourceRequest.setStatus(status);
        when(purchaseRequestRepository.findByIdAndOrganizationId(
                sourceRequest.getId(), organizationId)).thenReturn(Optional.of(sourceRequest));
        return sourceRequest;
    }

    private PurchaseRequestItem sourceItem(PurchaseRequest request, String name, int sortOrder) {
        PurchaseRequestItem item = new PurchaseRequestItem();
        item.setId(UUID.randomUUID());
        item.setPurchaseRequest(request);
        item.setItemName(name);
        item.setQuantity(new BigDecimal("20.000"));
        item.setUnit("PCS");
        item.setSortOrder(sortOrder);
        return item;
    }

    private Rfq storedRfq(RfqStatus status) {
        Rfq rfq = new Rfq();
        rfq.setId(UUID.randomUUID());
        rfq.setOrganization(organization);
        rfq.setRfqNumber("RFQ-2026-0001");
        rfq.setTitle("Laptops");
        rfq.setStatus(status);
        when(rfqRepository.findByIdAndOrganizationId(rfq.getId(), organizationId))
                .thenReturn(Optional.of(rfq));
        return rfq;
    }

    private com.vendorsphere.rfq.entity.RfqVendor invitation(UUID vendorId) {
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setCompanyName("Vendor " + vendorId.toString().substring(0, 8));
        com.vendorsphere.rfq.entity.RfqVendor invitation =
                new com.vendorsphere.rfq.entity.RfqVendor();
        invitation.setVendor(vendor);
        return invitation;
    }
}
