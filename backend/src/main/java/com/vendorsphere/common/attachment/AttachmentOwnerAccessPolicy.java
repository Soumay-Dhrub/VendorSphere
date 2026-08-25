package com.vendorsphere.common.attachment;

import java.util.UUID;

public interface AttachmentOwnerAccessPolicy {

    AttachmentOwnerType ownerType();

    void assertAccessible(UUID ownerId);
}
