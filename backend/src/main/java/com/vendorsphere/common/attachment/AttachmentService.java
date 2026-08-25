package com.vendorsphere.common.attachment;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface AttachmentService {

    long MAX_BYTE_SIZE = 10_485_760L;

    String SIZE_LIMIT_MESSAGE = "File exceeds the 10 MB limit";

    List<String> ACCEPTED_CONTENT_TYPES = List.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    AttachmentResponse upload(AttachmentOwnerType ownerType, UUID ownerId, MultipartFile file);

    List<AttachmentResponse> list(AttachmentOwnerType ownerType, UUID ownerId);

    AttachmentDownload download(UUID attachmentId);

    void delete(UUID attachmentId);
}
