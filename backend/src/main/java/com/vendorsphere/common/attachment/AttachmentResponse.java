package com.vendorsphere.common.attachment;

import java.time.Instant;
import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        AttachmentOwnerType ownerType,
        UUID ownerId,
        String originalFilename,
        String contentType,
        long byteSize,
        UUID uploadedById,
        Instant uploadedAt
) {

    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getOwnerType(),
                attachment.getOwnerId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getByteSize(),
                attachment.getUploadedBy().getId(),
                attachment.getCreatedAt());
    }
}
