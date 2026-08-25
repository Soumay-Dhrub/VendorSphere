package com.vendorsphere.quotation.service;

import com.vendorsphere.audit.service.AuditService;
import com.vendorsphere.auth.security.UserPrincipal;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.util.ReferenceNumberGenerator;
import com.vendorsphere.notification.service.NotificationService;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.quotation.service.EvaluationCriteriaWeightService;
import com.vendorsphere.quotation.dto.QuotationSubmitRequest;
import com.vendorsphere.rfq.RfqStatus;
import com.vendorsphere.rfq.entity.Rfq;
import com.vendorsphere.rfq.entity.RfqItem;
import com.vendorsphere.rfq.repository.RfqItemRepository;
import com.vendorsphere.rfq.repository.RfqRepository;
import com.vendorsphere.rfq.repository.RfqVendorRepository;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.user.repository.UserRepository;
import com.vendorsphere.vendor.VendorStatus;
import com.vendorsphere.vendor.entity.Vendor;
import com.vendorsphere.vendor.repository.VendorRepository;
import com.vendorsphere.vendor.service.VendorAccessGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The pinned submission rules of Requirement 12: the closed window, the unpriced-item listing and
 * the validity-date floor.
 */
class QuotationServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-20T09:00:00Z");

    private final RfqRepository rfqRepository = mock(RfqRepository.class);
    private final RfqItemRepository rfqItemRepository = mock(RfqItemRepository.class);
    private final RfqVendorRepository rfqVendorRepository = mock(RfqVendorRepository.class);
    private final com.vendorsphere.quotation.repository.QuotationRepository
            quotationRepository = mock(com.vendorsphere.quotation.repository.QuotationRepository.class);
    private final com.vendorsphere.quotation.repository.QuotationItemRepository
            quotationItemRepository =
            mock(com.vendorsphere.quotation.repository.QuotationItemRepository.class);
    private final VendorRepository vendorRepository = mock(VendorRepository.class);
    private final ReferenceNumberGenerator referenceNumberGenerator =
            mock(ReferenceNumberGenerator.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final VendorAccessGuard vendorAccessGuard = mock(VendorAccessGuard.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    // A real instance over mocked repos - Mockito cannot mock the concrete service class.
    private final EvaluationCriteriaWeightService weightsService =
            new EvaluationCriteriaWeightService(
                    mock(com.vendorsphere.quotation.repository.EvaluationCriteriaWeightRepository.class),
                    mock(com.vendorsphere.organization.repository.OrganizationRepository.class),
                    userRepository);

    private final QuotationService service = new QuotationService(
            rfqRepository, rfqItemRepository, rfqVendorRepository,
            quotationRepository, quotationItemRepository, vendorRepository,
            referenceNumberGenerator, notificationService, auditService, vendorAccessGuard,
            Clock.fixed(NOW, ZoneOffset.UTC),
            mock(com.vendorsphere.quotation.repository.VendorEvaluationRepository.class),
            vendorRepository, weightsService, userRepository);

    private final UUID organizationId = UUID.randomUUID();
    private final UUID vendorId = UUID.randomUUID();
    private final UUID rfqId = UUID.randomUUID();
    private Organization organization;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(organizationId);

        User officer = new User();
        officer.setId(UUID.randomUUID());
        officer.setOrganization(organization);
        officer.setEmail("officer@demo-corp.com");
        officer.setPasswordHash("hash");
        var principal = new UserPrincipal(officer);
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(new org.springframework.security.authentication
                        .UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()));
        when(vendorAccessGuard.currentVendorId()).thenReturn(Optional.of(vendorId));
        when(referenceNumberGenerator.allocate(any(), any())).thenReturn("QUOT-2026-0009");
        when(rfqVendorRepository.findByRfqIdAndVendorId(any(), any()))
                .thenReturn(Optional.of(new com.vendorsphere.rfq.entity.RfqVendor()));
    }

    @AfterEach
    void clearContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    /** Requirement 12.9: at or after closing, submission is closed - pinned 409 message. */
    @Test
    void submittingAtOrAfterClosingIsRejected() {
        Rfq openButExpired = rfq(RfqStatus.OPEN, NOW.minusSeconds(60));

        assertThatThrownBy(() -> service.submit(openButExpired.getId(), request(laptopsItemId)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("RFQ is closed for quotation submission")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    /** A non-OPEN RFQ is equally closed to new quotations. */
    @Test
    void submittingToADraftRfqIsRejected() {
        rfq(RfqStatus.DRAFT, NOW.plusSeconds(3600));

        assertThatThrownBy(() -> service.submit(rfqId, request(laptopsItemId)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("RFQ is closed for quotation submission");
    }

    /** Requirement 12.3: the 400 lists every RFQ item name lacking a price. */
    @Test
    void anUnpricedRfqItemIsListedInTheRejection() {
        rfq(RfqStatus.OPEN, NOW.plusSeconds(3600));
        UUID monitorId = UUID.randomUUID();
        when(rfqItemRepository.findByRfqIdOrderBySortOrderAscIdAsc(rfqId))
                .thenReturn(List.of(item(laptopsItemId, "Laptop"), item(monitorId, "Monitor")));

        assertThatThrownBy(() -> service.submit(rfqId, request(laptopsItemId)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("No price supplied for: Monitor")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /** Requirement 12.6: validity may not expire before the window closes. */
    @Test
    void aValidityDateBeforeClosingIsRejected() {
        Rfq open = rfq(RfqStatus.OPEN, NOW.plusSeconds(86400));
        when(rfqItemRepository.findByRfqIdOrderBySortOrderAscIdAsc(rfqId))
                .thenReturn(List.of(item(laptopsItemId, "Laptop")));
        when(rfqVendorRepository.findByRfqIdAndVendorId(rfqId, vendorId))
                .thenReturn(Optional.of(new com.vendorsphere.rfq.entity.RfqVendor()));

        QuotationSubmitRequest earlyValidity = new QuotationSubmitRequest(
                List.of(new QuotationSubmitRequest.ItemLine(
                        laptopsItemId, BigDecimal.ONE, new BigDecimal("60000"),
                        null, null)),
                null, 7, null, null, 12,
                java.time.LocalDate.parse("2020-01-01"), null);

        assertThatThrownBy(() -> service.submit(open.getId(), earlyValidity))
                .isInstanceOf(BusinessException.class)
                .hasMessage(
                        "Quotation validity date must be on or after the RFQ closing date");
    }

    // ----- fixtures -----

    private static final UUID laptopsItemId = UUID.randomUUID();

    private Rfq rfq(RfqStatus status, Instant closing) {
        Rfq rfq = new Rfq();
        rfq.setId(rfqId);
        rfq.setOrganization(organization());
        rfq.setRfqNumber("RFQ-2026-0001");
        rfq.setTitle("Laptops");
        rfq.setStatus(status);
        rfq.setClosingDate(closing);
        when(rfqRepository.findByIdAndOrganizationId(rfqId, organizationId))
                .thenReturn(Optional.of(rfq));
        return rfq;
    }

    private Organization organization() {
        return organization;
    }

    private RfqItem item(UUID id, String name) {
        RfqItem item = new RfqItem();
        item.setId(id);
        item.setItemName(name);
        item.setQuantity(BigDecimal.TEN);
        return item;
    }

    private QuotationSubmitRequest request(UUID pricedItemId) {
        return new QuotationSubmitRequest(
                List.of(new QuotationSubmitRequest.ItemLine(
                        pricedItemId, BigDecimal.TEN, new BigDecimal("60000"), null, null)),
                null, null, null, null, null, null, null);
    }
}
