package com.vendorsphere.vendor.service;

import com.vendorsphere.auth.security.UserPrincipal;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.user.RoleName;
import com.vendorsphere.vendor.repository.VendorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the caller's linked vendor from {@code vendors.user_id} and answers both questions of
 * {@link VendorAccessGuard} from that single resolution.
 *
 * <h4>Vendor user is a role, not the presence of a link</h4>
 *
 * <p>Whether the caller is a vendor user is read from the principal's authorities, never from whether
 * a vendor row happens to name the account. Inferring it from the data would invert the failure mode:
 * a vendor account whose link was deleted would look internal and be handed every vendor's records.
 * Reading the role first means a missing link denies instead.
 *
 * <h4>Resolved once per request</h4>
 *
 * <p>A single request can consult the guard many times - once per record on a listing - so the
 * resolution is cached in a request attribute, keyed on the principal so a cached value can never be
 * read back for a different user. Outside a request there is nowhere to cache, which is the case for
 * scheduled jobs; the resolution then simply runs per call rather than failing, and jobs carry no
 * principal at all so they resolve to "internal" without touching the database.
 *
 * <h4>Not shared with the notification directory</h4>
 *
 * <p>{@code VendorUserDirectory} reads the same column but answers the opposite question: vendor to
 * its users, unscoped, many results, for notification fan-out. This guard goes user to vendor, scoped
 * to the caller's organization, expects one result and denies otherwise. Folding them together would
 * force one of them to carry semantics it does not want - a fan-out that must reject ambiguity, or a
 * security check that tolerates it - so they stay separate readers of one column.
 */
@Service
public class VendorAccessGuardImpl implements VendorAccessGuard {

    /** Requirement 30.9 wording, reused so a denial reads the same wherever it comes from. */
    static final String ACCESS_DENIED_MESSAGE = "Access denied";

    static final String CACHE_ATTRIBUTE = VendorAccessGuardImpl.class.getName() + ".callerScope";

    private final VendorRepository vendorRepository;

    public VendorAccessGuardImpl(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @Override
    public Optional<UUID> currentVendorId() {
        CallerScope scope = callerScope();
        if (!scope.vendorUser()) {
            return Optional.empty();
        }
        // Never empty for a vendor user: see the invariant on VendorAccessGuard.
        return Optional.of(scope.linkedVendorId().orElseThrow(VendorAccessGuardImpl::accessDenied));
    }

    @Override
    public void assertVendorVisible(UUID recordVendorId, String notFoundMessage) {
        CallerScope scope = callerScope();
        if (!scope.vendorUser()) {
            return;
        }
        UUID callerVendorId = scope.linkedVendorId().orElse(null);
        if (callerVendorId == null || !callerVendorId.equals(recordVendorId)) {
            throw new BusinessException(notFoundMessage, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * The caller's scope, resolved at most once per request.
     *
     * <p>The cached entry is discarded when it belongs to another principal, so the cache cannot
     * outlive the authentication it was resolved for.
     */
    private CallerScope callerScope() {
        UserPrincipal principal = SecurityUtils.findCurrentUser().orElse(null);
        if (principal == null) {
            return CallerScope.internal(null);
        }

        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return resolve(principal);
        }
        if (attributes.getAttribute(CACHE_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)
                instanceof CallerScope cached && principal.getId().equals(cached.userId())) {
            return cached;
        }
        CallerScope resolved = resolve(principal);
        attributes.setAttribute(CACHE_ATTRIBUTE, resolved, RequestAttributes.SCOPE_REQUEST);
        return resolved;
    }

    /**
     * Reads {@code vendors.user_id} for a vendor user, scoped to the caller's organization so a vendor
     * of another tenant is never resolved as the caller's own (Requirement 30.10). An internal caller
     * is decided from the role alone and issues no query.
     */
    private CallerScope resolve(UserPrincipal principal) {
        if (!SecurityUtils.hasRole(principal, RoleName.VENDOR)) {
            return CallerScope.internal(principal.getId());
        }
        return new CallerScope(principal.getId(), true, vendorRepository
                .findIdsByUserIdAndOrganizationId(principal.getId(), principal.getOrganizationId()));
    }

    private static BusinessException accessDenied() {
        return new BusinessException(ACCESS_DENIED_MESSAGE, HttpStatus.FORBIDDEN);
    }

    /**
     * One resolution of the caller: whether the VENDOR role is held, and which vendors the account is
     * linked to. Cached as a whole so the role decision and the link are always read together.
     */
    private record CallerScope(UUID userId, boolean vendorUser, List<UUID> linkedVendorIds) {

        static CallerScope internal(UUID userId) {
            return new CallerScope(userId, false, List.of());
        }

        /**
         * The one vendor this account speaks for, empty when none is linked or when more than one is.
         *
         * <p>More than one link is a data error the schema does not prevent. Picking one would silently
         * decide which vendor's data the account may see, so it is treated as no scope at all and
         * denied, matching the fail-closed rule the guard is built on.
         */
        Optional<UUID> linkedVendorId() {
            return linkedVendorIds.size() == 1
                    ? Optional.of(linkedVendorIds.get(0))
                    : Optional.empty();
        }
    }
}
