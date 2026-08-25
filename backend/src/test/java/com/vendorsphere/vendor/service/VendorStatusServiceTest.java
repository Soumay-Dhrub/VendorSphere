package com.vendorsphere.vendor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.service.AuditService;
import com.vendorsphere.auth.security.UserPrincipal;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.vendor.VendorStatus;
import com.vendorsphere.vendor.dto.VendorStatusChangeRequest;
import com.vendorsphere.vendor.dto.VendorStatusSnapshot;
import com.vendorsphere.vendor.entity.Vendor;
import com.vendorsphere.vendor.repository.VendorRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class VendorStatusServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-14T09:15:30Z");

    private final VendorRepository vendorRepository = mock(VendorRepository.class);
    private final AuditService auditService = mock(AuditService.class);

    private final VendorStatusService service =
            new VendorStatusService(vendorRepository, auditService);

    private final UUID organizationId = UUID.randomUUID();
    private Organization organization;

    @BeforeEach
    void authenticateCaller() {
        organization = new Organization();
        organization.setId(organizationId);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setOrganization(organization);
        user.setEmail("manager@demo-corp.com");
        user.setPasswordHash("hash");

        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(vendorRepository.save(any(Vendor.class))).thenAnswer(call -> call.getArgument(0));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aPermittedChangePersistsTheNewStatusAndTheReason() {
        Vendor vendor = storedVendor(VendorStatus.ACTIVE);

        VendorStatusSnapshot result = service.changeStatus(
                vendor.getId(), new VendorStatusChangeRequest(VendorStatus.SUSPENDED, "  Late deliveries  "));

        assertThat(vendor.getStatus()).isEqualTo(VendorStatus.SUSPENDED);
        assertThat(vendor.getStatusChangeReason()).isEqualTo("Late deliveries");
        assertThat(result).isEqualTo(new VendorStatusSnapshot(
                vendor.getId(), VendorStatus.SUSPENDED, "Late deliveries"));
        verify(vendorRepository).save(vendor);
    }

    @Test
    void aPermittedChangeRecordsPreviousStatusNewStatusAndReasonOnTheAuditEntry() {
        Vendor vendor = storedVendor(VendorStatus.ACTIVE);
        vendor.setStatusChangeReason("Reinstated after review");

        service.changeStatus(vendor.getId(),
                new VendorStatusChangeRequest(VendorStatus.BLACKLISTED, "Repeated quality failures"));

        ArgumentCaptor<Object> previous = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> current = ArgumentCaptor.forClass(Object.class);
        verify(auditService).record(
                eq(AuditAction.VENDOR_STATUS_CHANGED), eq("Vendor"), eq(vendor.getId()),
                previous.capture(), current.capture());

        assertThat(previous.getValue()).isEqualTo(new VendorStatusSnapshot(
                vendor.getId(), VendorStatus.ACTIVE, "Reinstated after review"));
        assertThat(current.getValue()).isEqualTo(new VendorStatusSnapshot(
                vendor.getId(), VendorStatus.BLACKLISTED, "Repeated quality failures"));
    }

    @ParameterizedTest
    @EnumSource(value = VendorStatus.class, names = {"SUSPENDED", "BLACKLISTED", "INACTIVE"})
    void aChangeToARestrictedStatusWithoutAReasonIsRejected(VendorStatus target) {
        Vendor vendor = storedVendor(VendorStatus.ACTIVE);

        assertThatThrownBy(() ->
                service.changeStatus(vendor.getId(), new VendorStatusChangeRequest(target, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Status change reason is required")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(vendor.getStatus()).isEqualTo(VendorStatus.ACTIVE);
        verify(vendorRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void aBlankReasonCountsAsNoReason() {
        Vendor vendor = storedVendor(VendorStatus.ACTIVE);

        assertThatThrownBy(() -> service.changeStatus(vendor.getId(),
                new VendorStatusChangeRequest(VendorStatus.INACTIVE, "   ")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Status change reason is required");
    }

    @Test
    void aChangeToActiveWithoutAReasonIsAccepted() {
        Vendor vendor = storedVendor(VendorStatus.SUSPENDED);
        vendor.setStatusChangeReason("Late deliveries");

        VendorStatusSnapshot result = service.changeStatus(
                vendor.getId(), new VendorStatusChangeRequest(VendorStatus.ACTIVE, null));

        assertThat(result.status()).isEqualTo(VendorStatus.ACTIVE);
        assertThat(result.reason()).isNull();
        // The stale reason of the earlier suspension does not survive the change.
        assertThat(vendor.getStatusChangeReason()).isNull();
    }

    @Test
    void aTransitionOutsideThePermittedTableIsRejected() {
        Vendor vendor = storedVendor(VendorStatus.PROSPECTIVE);

        assertThatThrownBy(() -> service.changeStatus(vendor.getId(),
                new VendorStatusChangeRequest(VendorStatus.BLACKLISTED, "Fraudulent documents")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot transition from PROSPECTIVE to BLACKLISTED")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(vendor.getStatus()).isEqualTo(VendorStatus.PROSPECTIVE);
        verify(vendorRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void aRejectedTransitionIsReportedBeforeTheMissingReason() {
        Vendor vendor = storedVendor(VendorStatus.BLACKLISTED);

        assertThatThrownBy(() -> service.changeStatus(vendor.getId(),
                new VendorStatusChangeRequest(VendorStatus.SUSPENDED, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot transition from BLACKLISTED to SUSPENDED")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void changingTheStatusOfAVendorOfAnotherOrganizationIsNotFound() {
        UUID foreignVendorId = UUID.randomUUID();
        when(vendorRepository.findByIdAndOrganizationId(foreignVendorId, organizationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(foreignVendorId,
                new VendorStatusChangeRequest(VendorStatus.SUSPENDED, "Late deliveries")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor not found")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(vendorRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    // ----- fixtures -----

    private Vendor storedVendor(VendorStatus status) {
        Vendor vendor = new Vendor();
        vendor.setId(UUID.randomUUID());
        vendor.setOrganization(organization);
        vendor.setVendorCode("VEN-2026-001");
        vendor.setCompanyName("Acme Supplies");
        vendor.setEmail("sales@acme.test");
        vendor.setStatus(status);
        vendor.setRating(BigDecimal.ZERO.setScale(2));
        vendor.setRegisteredAt(NOW);
        when(vendorRepository.findByIdAndOrganizationId(vendor.getId(), organizationId))
                .thenReturn(Optional.of(vendor));
        return vendor;
    }
}
