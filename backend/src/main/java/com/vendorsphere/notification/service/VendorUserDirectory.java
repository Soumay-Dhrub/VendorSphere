package com.vendorsphere.notification.service;

import java.util.List;
import java.util.UUID;

/**
 * Resolves the user accounts linked to a vendor, for vendor-addressed notification fan-out.
 *
 * <p>Deliberately narrow: the notification module needs user identifiers and nothing else about a
 * vendor. Keeping the dependency at this width means notifications do not wait on the vendor module
 * and will not have to change when it lands - the vendor module can contribute its own
 * implementation over the {@code Vendor} entity later, and this contract stays the same.
 */
public interface VendorUserDirectory {

    /**
     * Identifiers of the users linked to {@code vendorId}, empty when the vendor has no linked user
     * account or does not exist.
     */
    List<UUID> findUserIdsOfVendor(UUID vendorId);
}
