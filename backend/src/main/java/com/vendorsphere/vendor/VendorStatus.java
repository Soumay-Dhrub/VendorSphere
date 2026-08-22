package com.vendorsphere.vendor;

/**
 * Lifecycle status of a vendor.
 *
 * <p>Persisted as {@code VARCHAR} through {@code @Enumerated(EnumType.STRING)}, matching the
 * {@code chk_vendor_status} constraint on {@code vendors.status}. Permitted transitions are held by
 * {@link VendorStatusTransitions}.
 */
public enum VendorStatus {

    /** Registered but not yet qualified to participate in procurement. */
    PROSPECTIVE,

    /** Qualified and eligible for RFQ invitations. */
    ACTIVE,

    /** Temporarily barred from procurement participation. */
    SUSPENDED,

    /** Permanently barred from procurement participation. */
    BLACKLISTED,

    /** No longer trading with the organization. */
    INACTIVE
}
