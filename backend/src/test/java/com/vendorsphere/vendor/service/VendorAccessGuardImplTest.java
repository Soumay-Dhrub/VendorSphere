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

/**
 * The confidentiality rule of Requirements 2.7 and 30.8: a vendor user reaches its own vendor's
 * records and nothing else, an internal user is unaffected, and every uncertain case denies.
 */
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

    /**
     * Requirement 30.8 restricts vendor users, not internal ones: an officer reads every vendor's
     * records subject to their role grants, so the guard neither denies nor queries anything.
     */
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

    /** Requirement 2.7: the linked vendor is resolved and its own records are visible. */
    @Test
    void aVendorUserResolvesItsLinkedVendorAndSeesItsOwnRecords() {
        UserPrincipal principal = principal(RoleName.VENDOR);
        authenticate(principal);
        linkedVendors(principal, ownVendorId);

        assertThat(guard.currentVendorId()).contains(ownVendorId);
        assertThatCode(() -> guard.assertVendorVisible(ownVendorId, QUOTATION_NOT_FOUND))
                .doesNotThrowAnyException();
    }

    /**
     * Requirement 30.10: another vendor's record is reported as absent, with the caller's own pinned
     * wording, so it cannot be told apart from a record that does not exist.
     */
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

    /**
     * A VENDOR account with no {@code vendors.user_id} row fails closed. Reporting "no vendor" here
     * the way an internal caller does would hand the account every vendor's records, so the record
     * check denies and the scope lookup refuses outright rather than returning an empty scope a
     * listing filter would read as "no restriction".
     */
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

    /**
     * {@code vendors.user_id} carries no unique constraint, so two rows can name one account. Choosing
     * one would silently decide whose data the account reads, so an ambiguous link denies as well.
     */
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

    /**
     * A listing consults the guard once per row, so the link is read once per request however many
     * times it is asked for.
     */
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

    /** A second request re-resolves: the cache lives no longer than the request that filled it. */
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

    /**
     * Scheduled jobs call the same services without a request or a principal. The guard treats that as
     * an internal caller instead of failing, since a job is not a vendor and there is nothing to hide
     * from it.
     */
    @Test
    void outsideARequestAndWithoutAPrincipalTheGuardImposesNoRestriction() {
        assertThat(guard.currentVendorId()).isEmpty();
        assertThatCode(() -> guard.assertVendorVisible(otherVendorId, QUOTATION_NOT_FOUND))
                .doesNotThrowAnyException();

        verifyNoInteractions(vendorRepository);
    }

    /** Without a request there is nowhere to cache, so the guard resolves per call and still decides. */
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
