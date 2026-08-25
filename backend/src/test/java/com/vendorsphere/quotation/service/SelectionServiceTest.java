package com.vendorsphere.quotation.service;

import com.vendorsphere.audit.service.AuditService;
import com.vendorsphere.auth.security.UserPrincipal;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.notification.service.NotificationService;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.quotation.QuotationStatus;
import com.vendorsphere.quotation.entity.Quotation;
import com.vendorsphere.quotation.repository.QuotationRepository;
import com.vendorsphere.quotation.repository.VendorEvaluationRepository;
import com.vendorsphere.quotation.repository.VendorSelectionRepository;
import com.vendorsphere.rfq.RfqStatus;
import com.vendorsphere.rfq.entity.Rfq;
import com.vendorsphere.rfq.repository.RfqRepository;
import com.vendorsphere.rfq.repository.RfqVendorRepository;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.user.repository.UserRepository;
import com.vendorsphere.vendor.entity.Vendor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SelectionServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-20T09:00:00Z");

    private final RfqRepository rfqRepository = mock(RfqRepository.class);
    private final RfqVendorRepository rfqVendorRepository = mock(RfqVendorRepository.class);
    private final QuotationRepository quotationRepository =
            mock(QuotationRepository.class);
    private final VendorSelectionRepository selectionRepository =
            mock(VendorSelectionRepository.class);
    private final VendorEvaluationRepository evaluationRepository =
            mock(VendorEvaluationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AuditService auditService = mock(AuditService.class);

    // A real engine over mocked repos - Mockito cannot mock the concrete class.
    private final com.vendorsphere.analytics.service.PerformanceEngine performanceEngine =
            new com.vendorsphere.analytics.service.PerformanceEngine(
                    mock(com.vendorsphere.analytics.repository.AnalyticsQueryRepository.class),
                    mock(com.vendorsphere.analytics.repository.VendorPerformanceSnapshotRepository.class),
                    mock(com.vendorsphere.vendor.repository.VendorRepository.class),
                    mock(com.vendorsphere.organization.repository.OrganizationRepository.class),
                    Clock.fixed(NOW, ZoneOffset.UTC));

    private final SelectionService service = new SelectionService(
            rfqRepository, rfqVendorRepository, quotationRepository, selectionRepository,
            evaluationRepository, userRepository, notificationService, auditService,
            performanceEngine, Clock.fixed(NOW, ZoneOffset.UTC));

    private final UUID organizationId = UUID.randomUUID();
    private final UUID rfqId = UUID.randomUUID();
    private final UUID winnerId = UUID.randomUUID();
    private Organization organization;
    private Rfq rfq;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(organizationId);

        User manager = new User();
        manager.setId(UUID.randomUUID());
        manager.setOrganization(organization);
        manager.setEmail("manager@demo-corp.com");
        manager.setPasswordHash("hash");
        when(userRepository.getReferenceById(any())).thenReturn(manager);
        var principal = new UserPrincipal(manager);
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(new org.springframework.security.authentication
                        .UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()));

        rfq = new Rfq();
        rfq.setId(rfqId);
        rfq.setOrganization(organization);
        rfq.setRfqNumber("RFQ-2026-0001");
        rfq.setStatus(RfqStatus.CLOSED);
        when(rfqRepository.findByIdAndOrganizationId(rfqId, organizationId))
                .thenReturn(Optional.of(rfq));
    }

    @AfterEach
    void clearContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void selectionWithoutAJustificationIsRejected() {
        assertThatThrownBy(() -> service.select(rfqId, winnerId, "   "))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Selection justification is required")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(rfq.getStatus()).isEqualTo(RfqStatus.CLOSED);
    }

    @Test
    void aSecondAwardIsRejected() {
        when(selectionRepository.existsByRfqId(rfqId)).thenReturn(true);

        assertThatThrownBy(() -> service.select(rfqId, winnerId, "Best overall value"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("RFQ is already awarded")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void selectingARejectedQuotationNamesItsStatus() {
        when(selectionRepository.existsByRfqId(rfqId)).thenReturn(false);
        storedWinner(QuotationStatus.REJECTED);

        assertThatThrownBy(() -> service.select(rfqId, winnerId, "Second thoughts"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot select a REJECTED quotation")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void selectionFlipsTheAwardStateAndPersistsTheRecord() {
        when(selectionRepository.existsByRfqId(rfqId)).thenReturn(false);
        Quotation winner = storedWinner(QuotationStatus.SUBMITTED);
        Quotation runnerUp = storedQuotation(UUID.randomUUID(), QuotationStatus.SUBMITTED);
        when(quotationRepository.findByRfqId(rfqId))
                .thenReturn(java.util.List.of(winner, runnerUp));
        when(rfqVendorRepository.findByRfqIdOrderByInvitedAtAsc(rfqId))
                .thenReturn(java.util.List.of());

        service.select(rfqId, winner.getId(), "Best combined price, delivery and history");

        assertThat(winner.getStatus()).isEqualTo(QuotationStatus.SELECTED);
        assertThat(runnerUp.getStatus()).isEqualTo(QuotationStatus.REJECTED);
        assertThat(rfq.getStatus()).isEqualTo(RfqStatus.AWARDED);
        verify(selectionRepository).save(any());
        verify(auditService).record(
                any(com.vendorsphere.audit.AuditAction.class), any(), any(), any(), any());
    }

    // ----- fixtures -----

    private Quotation storedWinner(QuotationStatus status) {
        return storedQuotation(winnerId, status);
    }

    private Quotation storedQuotation(UUID id, QuotationStatus status) {
        Vendor vendor = new Vendor();
        vendor.setId(UUID.randomUUID());
        vendor.setCompanyName("Acme Supplies");

        Quotation quotation = new Quotation();
        quotation.setId(id);
        quotation.setRfq(rfq);
        quotation.setVendor(vendor);
        quotation.setStatus(status);
        when(quotationRepository.findByIdAndRfqOrganizationId(id, organizationId))
                .thenReturn(Optional.of(quotation));
        return quotation;
    }
}
