package com.vendorsphere.common.attachment;

import org.springframework.core.io.Resource;

/**
 * A stored file together with the metadata needed to serve it. The file name is the sanitized
 * original name held as metadata, not the storage reference.
 */
public record AttachmentDownload(
        String filename,
        String contentType,
        long byteSize,
        Resource resource
) {
}
