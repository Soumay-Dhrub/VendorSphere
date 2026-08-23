package com.vendorsphere.vendor.dto;

import com.vendorsphere.vendor.VendorStatus;
import com.vendorsphere.vendor.entity.Vendor;

import java.util.UUID;

/**
 * The audited state of a vendor's status, used as the previous and new value of the
 * {@code VENDOR_STATUS_CHANGED} trail entry, so the pair carries the previous status, the new status
 * and the reason Requirement 3.3 asks for.
 *
 * <p>A purpose-built record rather than the {@link Vendor} entity, for the same reason
 * {@link VendorProfileSnapshot} is one: serializing an entity would drag its lazy associations into
 * {@code audit_logs}. It stays narrow as well, so a status change records only the state it touched
 * rather than the whole profile.
 *
 * <p>It doubles as the result of a status change, which is exactly the state a caller needs back.
 */
public record VendorStatusSnapshot(
        UUID vendorId,
        VendorStatus status,
        String reason
) {

    public static VendorStatusSnapshot from(Vendor vendor) {
        return new VendorStatusSnapshot(
                vendor.getId(), vendor.getStatus(), vendor.getStatusChangeReason());
    }
}
