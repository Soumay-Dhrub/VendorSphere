package com.vendorsphere.common.attachment;

import org.springframework.core.io.Resource;

public record AttachmentDownload(
        String filename,
        String contentType,
        long byteSize,
        Resource resource
) {
}
