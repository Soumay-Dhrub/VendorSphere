package com.vendorsphere.common.attachment;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Byte store behind {@link AttachmentService}. Implementations own the storage reference format and
 * must never derive it from caller-supplied text (Requirement 33.5).
 */
public interface AttachmentStorage {

    /**
     * Writes the uploaded bytes and returns the storage reference that addresses them.
     *
     * @return an opaque reference containing a randomly generated identifier and no part of the
     *         original file name
     */
    String store(MultipartFile file);

    /** Reads the bytes previously written under {@code storageReference}. */
    Resource load(String storageReference);

    /** Removes the bytes under {@code storageReference}; a missing file is not an error. */
    void delete(String storageReference);
}
