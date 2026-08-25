package com.vendorsphere.common.attachment;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface AttachmentStorage {

    String store(MultipartFile file);

    Resource load(String storageReference);

    void delete(String storageReference);
}
