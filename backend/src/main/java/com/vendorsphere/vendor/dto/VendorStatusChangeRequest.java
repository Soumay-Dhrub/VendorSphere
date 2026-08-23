package com.vendorsphere.vendor.dto;

import com.vendorsphere.vendor.VendorStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Status change payload for a vendor (Requirements 3.3, 3.4).
 *
 * <p>{@code status} is the target status and is mandatory, so an omitted or unparseable value is
 * answered as 400 at the DTO boundary rather than reaching the service.
 *
 * <p>{@code reason} is optional here on purpose: it is mandatory only for the three targets
 * Requirement 3.4 names (SUSPENDED, BLACKLISTED, INACTIVE), a condition bean validation cannot
 * express on a single component. {@code VendorStatusService} enforces it and pins the message
 * {@code Status change reason is required}. The component has no {@code Size} cap because
 * {@code vendors.status_change_reason} is a {@code TEXT} column.
 *
 * <p>The record carries no source status: the current status of the stored vendor is the only
 * trusted source of the transition's left-hand side (Requirements 3.1, 3.2).
 */
public record VendorStatusChangeRequest(
        @NotNull VendorStatus status,
        String reason
) {
}
