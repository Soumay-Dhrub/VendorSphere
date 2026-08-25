package com.vendorsphere.vendor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vendorsphere.auth.security.UserPrincipal;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.user.RoleName;
import com.vendorsphere.user.entity.Role;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.vendor.repository.VendorRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class VendorAccessGuardImplTest {

    private static final String QUOTATION_NOT_FOUND = "Quotation not found";
    private static final String PURCHASE_ORDER_NOT_FOUND = "Purchase order not found";

    private final VendorRepository vendorRepository = mock(VendorRepository.class);
    private final VendorAccessGuard guard = new VendorAccessGuardImpl(vendorRepository);

    private final UUID organizationId = UUID.randomUUID();
    private final UUID ownVendorId = UUID.randomUUID();
    private final UUID otherVendorId = UUID.randomUUID();

    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void anInternalUserIsUnrestrictedAndResolvesNoVendorAtAll() {
        authenticate(principal(RoleName.PROCUREMENT_OFFICER));

        assertThat(guard.currentVendorId()).isEmpty();
        assertThatCode(() -> guard.assertVendorVisible(ownVendorId, QUOTATION_NOT_FOUND))
                .doesNotThrowAnyException();
        assertThatCode(() -> guard.assertVendorVisible(otherVendorId, QUOTATION_NOT_FOUND))
                .doesNotThrowAnyException();
        assertThatCode(() -> guard.assertVendorVisible(null, QUOTATION_NOT_FOUND))
                .doesNotThrowAnyException();

        verifyNoInteractions(vendorRepository);
    }

    @Test
    void aVendorUserResolvesItsLinkedVendorAndSeesItsOwnRecords() {
        UserPrincipal principal = principal(RoleName.VENDOR);
        authenticate(principal);
        linkedVendors(principal, ownVendorId);

        assertThat(guard.currentVendorId()).contains(ownVendorId);
        assertThatCode(() -> guard.assertVendorVisible(ownVendorId, QUOTATION_NOT_FOUND))
                .doesNotThrowAnyException();
    }

    @Test
    void anotherVendorsRecordIsNotFoundUnderTheCallerSuppliedMessage() {
        UserPrincipal principal = principal(RoleName.VENDOR);
        authenticate(principal);
        linkedVendors(principal, ownVendorId);

        assertNotFound(() -> guard.assertVendorVisible(otherVendorId, QUOTATION_NOT_FOUND),
                QUOTATION_NOT_FOUND);
        assertNotFound(() -> guard.assertVendorVisible(otherVendorId, PURCHASE_ORDER_NOT_FOUND),
                PURCHASE_ORDER_NOT_FOUND);
        // A record belonging to no vendor is not this vendor's record either.
        assertNotFound(() -> guard.assertVendorVisible(null, QUOTATION_NOT_FOUND),
                QUOTATION_NOT_FOUND);
    }

    @Test
    void aVendorUserWithNoLinkedVendorIsDeniedRatherThanTreatedAsInternal() {
        UserPrincipal principal = principal(RoleName.VENDOR);
        authenticate(principal);
        linkedVendors(principal);

        assertNotFound(() -> guard.assertVendorVisible(ownVendorId, QUOTATION_NOT_FOUND),
                QUOTATION_NOT_FOUND);
        assertThatThrownBy(guard::currentVendorId)
                .isInstanceOf(BusinessException.class)
                .hasMessage(VendorAccessGuardImpl.ACCESS_DENIED_MESSAGE)
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void anAccountLinkedToMoreThanOneVendorIsDenied() {
        UserPrincipal principal = principal(RoleName.VENDOR);
        authenticate(principal);
        linkedVendors(principal, ownVendorId, otherVendorId);

        assertNotFound(() -> guard.assertVendorVisible(ownVendorId, QUOTATION_NOT_FOUND),
                QUOTATION_NOT_FOUND);
        assertThatThrownBy(guard::currentVendorId)
                .isInstanceOf(BusinessException.class)
                .hasMessage(VendorAccessGuardImpl.ACCESS_DENIED_MESSAGE);
    }

    @Test
    void theLinkedVendorIsResolvedOncePerRequest() {
        UserPrincipal principal = principal(RoleName.VENDOR);
        authenticate(principal);
        linkedVendors(principal, ownVendorId);
        inRequest();

        assertThat(guard.currentVendorId()).contains(ownVendorId);
        guard.assertVendorVisible(ownVendorId, QUOTATION_NOT_FOUND);
        guard.assertVendorVisible(ownVendorId, PURCHASE_ORDER_NOT_FOUND);
        assertThat(guard.currentVendorId()).contains(ownVendorId);

        verify(vendorRepository, times(1))
                .findIdsByUserIdAndOrganizationId(principal.getId(), organizationId);
    }

    @Test
    void aDifferentPrincipalInAFreshRequestIsResolvedAgain() {
        UserPrincipal firstVendor = principal(RoleName.VENDOR);
        authenticate(firstVendor);
        linkedVendors(firstVendor, ownVendorId);
        inRequest();
        assertThat(guard.currentVendorId()).contains(ownVendorId);

        UserPrincipal secondVendor = principal(RoleName.VENDOR);
        authenticate(secondVendor);
        linkedVendors(secondVendor, otherVendorId);
        inRequest();

        assertThat(guard.currentVendorId()).contains(otherVendorId);
        assertNotFound(() -> guard.assertVendorVisible(ownVendorId, QUOTATION_NOT_FOUND),
                QUOTATION_NOT_FOUND);
    }

    @Test
    void outsideARequestAndWithoutAPrincipalTheGuardImposesNoRestriction() {
        assertThat(guard.currentVendorId()).isEmpty();
        assertThatCode(() -> guard.assertVendorVisible(otherVendorId, QUOTATION_NOT_FOUND))
                .doesNotThrowAnyException();

        verifyNoInteractions(vendorRepository);
    }

    @Test
    void withoutARequestContextTheGuardResolvesPerCallRatherThanFailing() {
        UserPrincipal principal = principal(RoleName.VENDOR);
        authenticate(principal);
        linkedVendors(principal, ownVendorId);

        assertThat(guard.currentVendorId()).contains(ownVendorId);
        assertNotFound(() -> guard.assertVendorVisible(otherVendorId, QUOTATION_NOT_FOUND),
                QUOTATION_NOT_FOUND);

        verify(vendorRepository, times(2))
                .findIdsByUserIdAndOrganizationId(principal.getId(), organizationId);
    }

    // ----- fixtures -----

    private void assertNotFound(Runnable call, String message) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .hasMessage(message)
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private UserPrincipal principal(String roleName) {
        Organization organization = new Organization();
        organization.setId(organizationId);

        Role role = new Role();
        role.setName(roleName);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setOrganization(organization);
        user.setEmail("caller-" + UUID.randomUUID() + "@example.test");
        user.setPasswordHash("hash");
        user.setRoles(Set.of(role));
        return new UserPrincipal(user);
    }

    private void authenticate(UserPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private void linkedVendors(UserPrincipal principal, UUID... vendorIds) {
        when(vendorRepository.findIdsByUserIdAndOrganizationId(principal.getId(), organizationId))
                .thenReturn(List.of(vendorIds));
    }

    private void inRequest() {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest()));
    }
}
