package com.vendorsphere.common.util;

import java.util.UUID;

/**
 * Allocates human-readable business identifiers such as vendor codes, purchase request numbers,
 * RFQ numbers, purchase order numbers and delivery numbers.
 *
 * <p>Allocation joins the caller's transaction: the number is consumed exactly when the record that
 * carries it commits, and released when that record rolls back (Requirement 1.5).
 */
public interface ReferenceNumberGenerator {

    /**
     * Allocates the next number for the organization, prefix and current year inside the caller's
     * transaction.
     *
     * @param organizationId the owning organization
     * @param prefix the record type prefix
     * @return a reference number of the form {@code {PREFIX}-{YYYY}-{NNN}}, starting at {@code 001}
     *     for the first allocation of an organization, prefix and year
     */
    String allocate(UUID organizationId, ReferencePrefix prefix);
}
