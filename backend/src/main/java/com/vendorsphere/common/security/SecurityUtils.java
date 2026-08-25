package com.vendorsphere.common.security;

import com.vendorsphere.auth.security.UserPrincipal;
import com.vendorsphere.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UserPrincipal> findCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    public static UserPrincipal getCurrentUser() {
        return findCurrentUser().orElseThrow(
                () -> new BusinessException("Not authenticated", HttpStatus.UNAUTHORIZED));
    }

    public static UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static UUID getCurrentOrganizationId() {
        return getCurrentUser().getOrganizationId();
    }

    public static boolean hasRole(String role) {
        return hasRole(getCurrentUser(), role);
    }

    public static boolean hasRole(UserPrincipal principal, String role) {
        return principal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role));
    }
}
