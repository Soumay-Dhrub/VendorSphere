package com.vendorsphere.vendor.service;

import com.vendorsphere.common.attachment.AttachmentOwnerAccessPolicy;
import com.vendorsphere.common.attachment.AttachmentOwnerType;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Opens {@code VENDOR_DOCUMENT} attachments to the same audience the vendor document listing serves.
 *
 * <p>Without a policy the attachment module falls back to tenant scoping alone, which would let any
 * internal user download any vendor's compliance file. Requirement 30.8 is narrower than that for
 * vendor users - linked documents only - so this policy delegates the question to
 * {@link VendorAccessGuard#assertVendorVisible} with the document list's own not-found wording, and a
 * denial is indistinguishable from an unknown attachment.
 */
@Component
public class VendorDocumentAttachmentPolicy implements AttachmentOwnerAccessPolicy {

    static final String NOT_FOUND_MESSAGE = "Vendor document not found";

    private final VendorAccessGuard vendorAccessGuard;

    public VendorDocumentAttachmentPolicy(VendorAccessGuard vendorAccessGuard) {
        this.vendorAccessGuard = vendorAccessGuard;
    }

    @Override
    public AttachmentOwnerType ownerType() {
        return AttachmentOwnerType.VENDOR_DOCUMENT;
    }

    @Override
    public void assertAccessible(UUID ownerId) {
        // The attachment's owner id is the vendor id for this owner type.
        vendorAccessGuard.assertVendorVisible(ownerId, NOT_FOUND_MESSAGE);
    }
}
