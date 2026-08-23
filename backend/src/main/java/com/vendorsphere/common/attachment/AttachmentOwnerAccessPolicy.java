package com.vendorsphere.common.attachment;

import java.util.UUID;

/**
 * Access check for the record that owns an attachment (Requirement 33.6).
 *
 * <p>Tenant scoping alone cannot decide every case: a vendor user may only reach files hanging off
 * its own quotations, and a requester only its own purchase requests. Those rules live with the
 * owning module, so each module contributes one policy bean for the owner types it understands and
 * {@link AttachmentService} consults it before serving, listing or removing a file.
 *
 * <p>Implementations MUST fail like a tenant-scoped finder: raise a 404 rather than a 403 so
 * identifiers stay non-enumerable.
 */
public interface AttachmentOwnerAccessPolicy {

    /** The owner type this policy governs. */
    AttachmentOwnerType ownerType();

    /**
     * Throws when the current actor may not see the owning record.
     *
     * @param ownerId identifier of the owning record, already known to belong to the actor's
     *                organization
     */
    void assertAccessible(UUID ownerId);
}
