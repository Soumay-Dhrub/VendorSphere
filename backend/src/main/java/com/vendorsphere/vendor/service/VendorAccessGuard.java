package com.vendorsphere.vendor.service;

import java.util.Optional;
import java.util.UUID;

/**
 * The single place vendor confidentiality is decided (Requirements 2.7, 30.8, 30.10).
 *
 * <p>Role annotations answer "may a VENDOR user reach this endpoint at all"; they cannot answer "is
 * this record the caller's". Every module whose records belong to a vendor - quotations, purchase
 * orders, deliveries, invoices - delegates that second question here, so one implementation decides
 * it for all of them and a new module cannot invent a weaker rule.
 *
 * <h4>The contract, stated as an invariant</h4>
 *
 * <p>{@link #currentVendorId()} is empty <em>only</em> when the caller is not a vendor user. It is
 * never empty for a vendor user. That matters because callers filter listings with it, typically as
 * "empty means show everything". If a vendor user could ever produce an empty result the filter would
 * open up instead of closing down, which is precisely the confidentiality hole this guard exists to
 * prevent. A vendor user whose account has no linked vendor row therefore fails closed: the account
 * is misconfigured, and denying it costs one portal user an error message while allowing it would
 * expose every vendor's commercial data to that account.
 *
 * <p>Both methods are safe to call outside a request - a scheduled job carries no principal, so it is
 * treated as an internal caller and no vendor restriction applies.
 *
 * <h4>The vendor profile itself</h4>
 *
 * <p>Requirement 2.7 restricts a vendor user to the linked vendor profile. That is the same rule with
 * the record's vendor id being the profile's own id, so a profile read or update calls
 * {@code assertVendorVisible(vendorId, "Vendor not found")} after the tenant-scoped load and needs no
 * separate mechanism.
 */
public interface VendorAccessGuard {

    /**
     * Vendor id linked to the current user, or empty when the caller is an internal user.
     *
     * @throws com.vendorsphere.common.exception.BusinessException 403 {@code Access denied} when the
     *         caller holds the VENDOR role but no vendor is linked to the account, or when the account
     *         is linked to more than one vendor and the intended scope is therefore ambiguous
     */
    Optional<UUID> currentVendorId();

    /**
     * Throws 404 when the caller is a vendor user and the record belongs to another vendor.
     *
     * <p>A no-op for internal users: they legitimately read every vendor's records, subject to the
     * role grants of Requirements 30.3 through 30.7. 404 rather than 403 because a vendor user must
     * not be able to tell another vendor's record apart from one that does not exist (Requirement
     * 30.10), which is also why the message is the caller's own pinned not-found wording rather than
     * anything describing the check.
     *
     * @param recordVendorId  the vendor the record belongs to; {@code null} is not visible to a vendor
     *                        user, since a record with no vendor is not that vendor's record
     * @param notFoundMessage the message to report, so each module produces its own pinned wording
     *                        ({@code Quotation not found}, {@code Purchase order not found}, ...)
     */
    void assertVendorVisible(UUID recordVendorId, String notFoundMessage);
}
