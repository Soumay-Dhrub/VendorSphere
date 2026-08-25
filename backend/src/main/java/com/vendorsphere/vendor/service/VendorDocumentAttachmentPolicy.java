package com.vendorsphere.vendor.service;

import com.vendorsphere.common.attachment.AttachmentOwnerAccessPolicy;
import com.vendorsphere.common.attachment.AttachmentOwnerType;
import org.springframework.stereotype.Component;

import java.util.UUID;

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
