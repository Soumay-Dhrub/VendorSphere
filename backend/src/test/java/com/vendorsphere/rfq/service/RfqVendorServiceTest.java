package com.vendorsphere.rfq.service;

import com.vendorsphere.audit.service.AuditService;
import com.vendorsphere.auth.security.UserPrincipal;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.notification.service.NotificationService;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.rfq.RfqStatus;
import com.vendorsphere.rfq.RfqVendorStatus;
import com.vendorsphere.rfq.dto.RfqInviteRequest;
import com.vendorsphere.rfq.entity.Rfq;
import com.vendorsphere.rfq.entity.RfqVendor;
import com.vendorsphere.rfq.repository.RfqRepository;
import com.vendorsphere.rfq.repository.RfqVendorRepository;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.user.repository.UserRepository;
import com.vendorsphere.vendor.VendorStatus;
import com.vendorsphere.vendor.entity.Vendor;
import com.vendorsphere.vendor.repository.VendorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RfqVendorServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-14T09:15:30Z");

    private final RfqVendorRepository rfqVendorRepository = mock(RfqVendorRepository.class);
    private final RfqRepository rfqRepository = mock(RfqRepository.class);
    private final VendorRepository vendorRepository = mock(VendorRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AuditService auditService = mock(AuditService.class);

    private final RfqVendorService service = new RfqVendorService(
            rfqVendorRepository, rfqRepository, vendorRepository, userRepository,
            notificationService, auditService, Clock.fixed(NOW, ZoneOffset.UTC));

    private final UUID organizationId = UUID.randomUUID();
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
        when(userRepository.getReferenceById(any(UUID.class))).thenReturn(officer);

        var principal = new UserPrincipal(officer);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(rfqVendorRepository.save(any(RfqVendor.class))).thenAnswer(call -> call.getArgument(0));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void invitingAnInactiveVendorNamesTheCompanyAndStatus() {
        Rfq open = storedRfq(RfqStatus.OPEN);
        UUID suspendedId = UUID.randomUUID();
        vendor(suspendedId, "Slow Supplies", VendorStatus.SUSPENDED);

        assertThatThrownBy(() -> service.invite(open.getId(),
                new RfqInviteRequest(List.of(suspendedId))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor Slow Supplies is SUSPENDED")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(rfqVendorRepository, never()).save(any());
    }

    @Test
    void aBatchWithOneAlreadyInvitedVendorWritesNothing() {
        Rfq draft = storedRfq(RfqStatus.DRAFT);
        UUID alreadyInvitedId = UUID.randomUUID();
        UUID freshId = UUID.randomUUID();
        vendor(alreadyInvitedId, "Acme Supplies", VendorStatus.ACTIVE);
        vendor(freshId, "Fresh Parts", VendorStatus.ACTIVE);
        when(rfqVendorRepository.findByRfqIdAndVendorId(draft.getId(), alreadyInvitedId))
                .thenReturn(Optional.of(new RfqVendor()));

        assertThatThrownBy(() -> service.invite(draft.getId(),
                new RfqInviteRequest(List.of(alreadyInvitedId, freshId))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor already invited to this RFQ");

        verify(rfqVendorRepository, never()).save(any());
    }

    @Test
    void invitingIntoAnOpenRfqNotifiesTheVendorsUsers() {
        Rfq open = storedRfq(RfqStatus.OPEN);
        UUID vendorId = UUID.randomUUID();
        vendor(vendorId, "Acme Supplies", VendorStatus.ACTIVE);

        var created = service.invite(open.getId(), new RfqInviteRequest(List.of(vendorId)));

        assertThat(created).hasSize(1);
        assertThat(created.get(0).status()).isEqualTo(RfqVendorStatus.INVITED);
        verify(notificationService).createForVendorUsers(
                eq(vendorId), any(), eq("Rfq"), eq(open.getId()), any(), any());
    }

    @Test
    void invitingIntoADraftRfqNotifiesNobody() {
        Rfq draft = storedRfq(RfqStatus.DRAFT);
        UUID vendorId = UUID.randomUUID();
        vendor(vendorId, "Acme Supplies", VendorStatus.ACTIVE);

        service.invite(draft.getId(), new RfqInviteRequest(List.of(vendorId)));

        verify(notificationService, never()).createForVendorUsers(any(), any(), any(), any(),
                any(), any());
    }

    @Test
    void firstVendorReadMarksTheInvitationViewed() {
        Rfq open = storedRfq(RfqStatus.OPEN);
        UUID vendorId = UUID.randomUUID();
        RfqVendor invitation = invitationOf(open, vendor(vendorId, "Acme", VendorStatus.ACTIVE),
                RfqVendorStatus.INVITED);
        when(rfqVendorRepository.findByRfqIdAndVendorIdAndVendorOrganizationId(
                open.getId(), vendorId, organizationId)).thenReturn(Optional.of(invitation));

        service.getForVendor(organizationId, vendorId, open.getId());

        assertThat(invitation.getStatus()).isEqualTo(RfqVendorStatus.VIEWED);
    }

    @Test
    void aDraftRfqIsInvisibleToInvitedVendors() {
        Rfq draft = storedRfq(RfqStatus.DRAFT);
        UUID vendorId = UUID.randomUUID();
        RfqVendor invitation = invitationOf(draft, vendor(vendorId, "Acme", VendorStatus.ACTIVE),
                RfqVendorStatus.INVITED);
        when(rfqVendorRepository.findByRfqIdAndVendorIdAndVendorOrganizationId(
                draft.getId(), vendorId, organizationId)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> service.getForVendor(organizationId, vendorId, draft.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("RFQ not found");
    }

    // ----- fixtures -----

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

    private Vendor vendor(UUID id, String companyName, VendorStatus status) {
        Vendor vendor = new Vendor();
        vendor.setId(id);
        vendor.setOrganization(organization);
        vendor.setCompanyName(companyName);
        vendor.setStatus(status);
        when(vendorRepository.findByIdAndOrganizationId(id, organizationId))
                .thenReturn(Optional.of(vendor));
        return vendor;
    }

    private RfqVendor invitationOf(Rfq rfq, Vendor vendor, RfqVendorStatus status) {
        RfqVendor invitation = new RfqVendor();
        invitation.setRfq(rfq);
        invitation.setVendor(vendor);
        invitation.setStatus(status);
        return invitation;
    }
}
