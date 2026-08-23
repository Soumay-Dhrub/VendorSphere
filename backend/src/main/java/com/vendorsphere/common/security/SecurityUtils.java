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

    /**
     * The authenticated principal, or empty when the current thread carries no authenticated user.
     *
     * <p>Scheduled jobs run outside any request and therefore outside any security context, yet they
     * call into the same services a request does. Code on those paths asks this question instead of
     * {@link #getCurrentUser()}, which would raise 401 for a caller that is not a user at all.
     */
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

    /**
     * Whether {@code principal} holds {@code role}, for callers that already resolved the principal
     * through {@link #findCurrentUser()} and must not re-enter the throwing accessor.
     */
    public static boolean hasRole(UserPrincipal principal, String role) {
        return principal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role));
    }
}
