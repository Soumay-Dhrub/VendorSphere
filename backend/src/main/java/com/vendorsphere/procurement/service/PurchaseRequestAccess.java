package com.vendorsphere.procurement.service;

import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.procurement.entity.PurchaseRequest;
import com.vendorsphere.user.RoleName;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Who may read a purchase request, decided once and shared by every reader of the module
 * (Requirements 30.6, 8.9).
 *
 * <p>Internal procurement roles see every request of their organization; a caller holding none of
 * them - in practice a REQUESTER - is narrowed to the requests it authored itself, and any other
 * identifier surfaces as 404 {@code Purchase request not found} rather than 403, so identifiers stay
 * non-enumerable (Requirement 30.10).
 *
 * <p>The narrowing reads roles from the principal, never from the data: an account that gained or
 * lost an internal role sees the rule move with the role on its next request, and a requester cannot
 * widen the scope by passing another user's identifier.
 *
 * <p>Both methods are safe outside a request - scheduled jobs carry no principal and are treated as
 * internal callers.
 */
@Component
public class PurchaseRequestAccess {

    static final String NOT_FOUND_MESSAGE = "Purchase request not found";

    /** The roles that see every request of the organization (Requirements 30.3 through 30.5). */
    private static final Set<String> INTERNAL_ROLES = Set.of(
            RoleName.ADMIN,
            RoleName.PROCUREMENT_OFFICER,
            RoleName.PROCUREMENT_MANAGER);

    /**
     * Throws when the current caller may not see {@code request}. A no-op for internal callers and
     * for jobs; otherwise only the author passes.
     */
    public void assertReadable(PurchaseRequest request) {
        if (isInternalCaller()) {
            return;
        }
        UUID callerId = SecurityUtils.getCurrentUser().getId();
        if (!request.getRequester().getId().equals(callerId)) {
            throw new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * The user identifier a listing must be narrowed to, or empty when no narrowing applies.
     *
     * <p>Read once per listing so the specification receives one authoritative value rather than
     * re-deriving it per predicate.
     */
    public UUID restrictedRequesterId() {
        return isInternalCaller() ? null : SecurityUtils.getCurrentUserId();
    }

    private boolean isInternalCaller() {
        return SecurityUtils.findCurrentUser()
                .map(principal -> INTERNAL_ROLES.stream()
                        .anyMatch(role -> SecurityUtils.hasRole(principal, role)))
                .orElse(true);
    }
}
