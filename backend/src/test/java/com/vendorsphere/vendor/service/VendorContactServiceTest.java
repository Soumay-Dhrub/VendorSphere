package com.vendorsphere.vendor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vendorsphere.auth.security.UserPrincipal;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.vendor.VendorStatus;
import com.vendorsphere.vendor.dto.VendorContactRequest;
import com.vendorsphere.vendor.dto.VendorContactResponse;
import com.vendorsphere.vendor.entity.Vendor;
import com.vendorsphere.vendor.entity.VendorContact;
import com.vendorsphere.vendor.repository.VendorContactRepository;
import com.vendorsphere.vendor.repository.VendorRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class VendorContactServiceTest {

    private final VendorContactRepository vendorContactRepository =
            mock(VendorContactRepository.class);
    private final VendorRepository vendorRepository = mock(VendorRepository.class);

    private final VendorContactService service =
            new VendorContactService(vendorContactRepository, vendorRepository);

    private final UUID organizationId = UUID.randomUUID();
    private Organization organization;
    private Vendor vendor;

    @BeforeEach
    void authenticateCaller() {
        organization = new Organization();
        organization.setId(organizationId);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setOrganization(organization);
        user.setEmail("officer@demo-corp.com");
        user.setPasswordHash("hash");

        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        vendor = new Vendor();
        vendor.setId(UUID.randomUUID());
        vendor.setOrganization(organization);
        vendor.setVendorCode("VEN-2026-001");
        vendor.setCompanyName("Acme Supplies");
        vendor.setEmail("sales@acme.test");
        vendor.setStatus(VendorStatus.PROSPECTIVE);
        vendor.setRating(BigDecimal.ZERO.setScale(2));
        vendor.setRegisteredAt(Instant.parse("2026-03-14T09:15:30Z"));

        when(vendorRepository.findByIdAndOrganizationId(vendor.getId(), organizationId))
                .thenReturn(Optional.of(vendor));
        when(vendorContactRepository.save(any(VendorContact.class))).thenAnswer(invocation -> {
            VendorContact contact = invocation.getArgument(0);
            if (contact.getId() == null) {
                contact.setId(UUID.randomUUID());
            }
            return contact;
        });
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addingAPrimaryContactClearsTheFlagOnTheVendorsOtherContacts() {
        VendorContact incumbent = contact("Riya Nair", true);
        VendorContact secondary = contact("Arun Mehta", false);
        when(vendorContactRepository
                .findByVendorIdAndVendorOrganizationIdOrderByPrimaryContactDescNameAsc(
                        vendor.getId(), organizationId))
                .thenReturn(List.of(incumbent, secondary));

        VendorContactResponse response = service.add(vendor.getId(),
                new VendorContactRequest("Nisha Rao", "nisha@acme.test", "+91-9000000001",
                        "Key Account Manager", true));

        assertThat(response.primaryContact()).isTrue();
        assertThat(response.vendorId()).isEqualTo(vendor.getId());
        assertThat(incumbent.isPrimaryContact()).isFalse();
        // The contact that never held the flag is not rewritten just to leave it false.
        assertThat(secondary.isPrimaryContact()).isFalse();
        verify(vendorContactRepository).save(incumbent);
        verify(vendorContactRepository, never()).save(secondary);
    }

    @Test
    void addingANonPrimaryContactPromotesNothingAndDemotesNothing() {
        VendorContact incumbent = contact("Riya Nair", true);
        when(vendorContactRepository
                .findByVendorIdAndVendorOrganizationIdOrderByPrimaryContactDescNameAsc(
                        vendor.getId(), organizationId))
                .thenReturn(List.of(incumbent));

        VendorContactResponse response = service.add(vendor.getId(),
                new VendorContactRequest("Nisha Rao", null, null, null, false));

        assertThat(response.primaryContact()).isFalse();
        assertThat(incumbent.isPrimaryContact()).isTrue();
        verify(vendorContactRepository, never()).save(incumbent);
    }

    @Test
    void promotingAnExistingContactDemotesTheOthersButNotItself() {
        VendorContact promoted = contact("Nisha Rao", false);
        VendorContact incumbent = contact("Riya Nair", true);
        when(vendorContactRepository.findByIdAndVendorOrganizationId(promoted.getId(), organizationId))
                .thenReturn(Optional.of(promoted));
        when(vendorContactRepository
                .findByVendorIdAndVendorOrganizationIdOrderByPrimaryContactDescNameAsc(
                        vendor.getId(), organizationId))
                .thenReturn(List.of(incumbent, promoted));

        VendorContactResponse response = service.update(vendor.getId(), promoted.getId(),
                new VendorContactRequest("Nisha Rao", "nisha@acme.test", null, "Director", true));

        assertThat(response.primaryContact()).isTrue();
        assertThat(response.designation()).isEqualTo("Director");
        assertThat(promoted.isPrimaryContact()).isTrue();
        assertThat(incumbent.isPrimaryContact()).isFalse();
    }

    @Test
    void listReturnsTheContactsInPrimaryFirstThenNameAscendingOrder() {
        VendorContact primary = contact("Riya Nair", true);
        VendorContact arun = contact("Arun Mehta", false);
        VendorContact nisha = contact("Nisha Rao", false);
        when(vendorContactRepository
                .findByVendorIdAndVendorOrganizationIdOrderByPrimaryContactDescNameAsc(
                        vendor.getId(), organizationId))
                .thenReturn(List.of(primary, arun, nisha));

        assertThat(service.list(vendor.getId()))
                .extracting(VendorContactResponse::name)
                .containsExactly("Riya Nair", "Arun Mehta", "Nisha Rao");
    }

    @Test
    void contactsOfAVendorOfAnotherOrganizationAreNotFound() {
        UUID foreignVendorId = UUID.randomUUID();
        when(vendorRepository.findByIdAndOrganizationId(foreignVendorId, organizationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.list(foreignVendorId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor not found")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThatThrownBy(() -> service.add(foreignVendorId,
                new VendorContactRequest("Nisha Rao", null, null, null, true)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor not found");

        verify(vendorContactRepository, never()).save(any());
    }

    @Test
    void aContactBelongingToAnotherVendorIsNotFound() {
        Vendor otherVendor = new Vendor();
        otherVendor.setId(UUID.randomUUID());
        otherVendor.setOrganization(organization);

        VendorContact foreign = new VendorContact();
        foreign.setId(UUID.randomUUID());
        foreign.setVendor(otherVendor);
        foreign.setName("Riya Nair");
        when(vendorContactRepository.findByIdAndVendorOrganizationId(foreign.getId(), organizationId))
                .thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.update(vendor.getId(), foreign.getId(),
                new VendorContactRequest("Renamed", null, null, null, false)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor contact not found")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        UUID unknownContactId = UUID.randomUUID();
        when(vendorContactRepository.findByIdAndVendorOrganizationId(unknownContactId, organizationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(vendor.getId(), unknownContactId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor contact not found");

        verify(vendorContactRepository, never()).delete(any());
    }

    private VendorContact contact(String name, boolean primary) {
        VendorContact contact = new VendorContact();
        contact.setId(UUID.randomUUID());
        contact.setVendor(vendor);
        contact.setName(name);
        contact.setPrimaryContact(primary);
        return contact;
    }
}
