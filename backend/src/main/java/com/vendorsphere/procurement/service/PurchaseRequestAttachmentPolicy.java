package com.vendorsphere.procurement.service;

import com.vendorsphere.common.attachment.AttachmentOwnerAccessPolicy;
import com.vendorsphere.common.attachment.AttachmentOwnerType;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.procurement.entity.PurchaseRequest;
import com.vendorsphere.procurement.repository.PurchaseRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Opens {@code PURCHASE_REQUEST} attachments to the same audience the request reads serve
 * (Requirement 33.6).
 *
 * <p>Without a policy, tenant scoping would be the only gate on a requester's supporting files.
 * Requirement 30.6 narrows that: a REQUESTER-only account reads only its own requests, so it
 * downloads its own attachments and no others. The check loads the owning request tenant-scoped and
 * delegates to {@link PurchaseRequestAccess} - the same rule every request read applies - so a denial
 * is indistinguishable from an unknown attachment.
 */
@Component
public class PurchaseRequestAttachmentPolicy implements AttachmentOwnerAccessPolicy {

    static final String NOT_FOUND_MESSAGE = PurchaseRequestAccess.NOT_FOUND_MESSAGE;

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestAccess purchaseRequestAccess;

    public PurchaseRequestAttachmentPolicy(
            PurchaseRequestRepository purchaseRequestRepository,
            PurchaseRequestAccess purchaseRequestAccess
    ) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.purchaseRequestAccess = purchaseRequestAccess;
    }

    @Override
    public AttachmentOwnerType ownerType() {
        return AttachmentOwnerType.PURCHASE_REQUEST;
    }

    @Override
    public void assertAccessible(UUID ownerId) {
        // The attachment's owner id is the purchase request id for this owner type.
        PurchaseRequest request = purchaseRequestRepository
                .findByIdAndOrganizationId(ownerId, SecurityUtils.getCurrentOrganizationId())
                .orElseThrow(() -> new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        purchaseRequestAccess.assertReadable(request);
    }
}
