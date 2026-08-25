package com.vendorsphere.procurement.service;

import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.procurement.entity.PurchaseRequest;
import com.vendorsphere.user.RoleName;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class PurchaseRequestAccess {

    static final String NOT_FOUND_MESSAGE = "Purchase request not found";

    private static final Set<String> INTERNAL_ROLES = Set.of(
            RoleName.ADMIN,
            RoleName.PROCUREMENT_OFFICER,
            RoleName.PROCUREMENT_MANAGER);

    public void assertReadable(PurchaseRequest request) {
        if (isInternalCaller()) {
            return;
        }
        UUID callerId = SecurityUtils.getCurrentUser().getId();
        if (!request.getRequester().getId().equals(callerId)) {
            throw new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND);
        }
    }

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
