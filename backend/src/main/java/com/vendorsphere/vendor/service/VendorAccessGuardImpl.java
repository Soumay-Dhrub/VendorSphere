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

@Service
public class VendorAccessGuardImpl implements VendorAccessGuard {

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

    private record CallerScope(UUID userId, boolean vendorUser, List<UUID> linkedVendorIds) {

        static CallerScope internal(UUID userId) {
            return new CallerScope(userId, false, List.of());
        }

        Optional<UUID> linkedVendorId() {
            return linkedVendorIds.size() == 1
                    ? Optional.of(linkedVendorIds.get(0))
                    : Optional.empty();
        }
    }
}
