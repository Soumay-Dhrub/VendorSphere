package com.vendorsphere.common.attachment;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Single entry point for file uploads across the platform (Requirement 33). Content-type
 * allowlisting, the size gate, storage-reference generation and owner access checks live here so
 * every module inherits the same rules.
 */
public interface AttachmentService {

    /** Largest accepted upload in bytes (Requirement 33.4). */
    long MAX_BYTE_SIZE = 10_485_760L;

    /** Pinned wording for an upload above {@link #MAX_BYTE_SIZE} (Requirement 33.4). */
    String SIZE_LIMIT_MESSAGE = "File exceeds the 10 MB limit";

    /** Content types the platform accepts, in the order Requirement 33.2 lists them. */
    List<String> ACCEPTED_CONTENT_TYPES = List.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    /**
     * Stores {@code file} against an owning record and returns its metadata.
     *
     * @throws com.vendorsphere.common.exception.BusinessException 415 when the content type is
     *         outside {@link #ACCEPTED_CONTENT_TYPES}, 413 when the byte size exceeds
     *         {@link #MAX_BYTE_SIZE}
     */
    AttachmentResponse upload(AttachmentOwnerType ownerType, UUID ownerId, MultipartFile file);

    /** Lists the attachments of one owning record, oldest first. */
    List<AttachmentResponse> list(AttachmentOwnerType ownerType, UUID ownerId);

    /** Reads a stored file after checking access to the owning record (Requirement 33.6). */
    AttachmentDownload download(UUID attachmentId);

    /** Removes an attachment and its bytes after checking access to the owning record. */
    void delete(UUID attachmentId);
}
