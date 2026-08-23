package com.vendorsphere.common.attachment;

import com.vendorsphere.common.config.AttachmentProperties;
import com.vendorsphere.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Local filesystem store for the MVP scope (Requirement 33.7).
 *
 * <p>A file is written as {@code {baseDirectory}/{randomUuid}} and the storage reference is that
 * UUID in text form. The original file name plays no part in path resolution, and every reference
 * is parsed as a UUID before it touches the filesystem, so an attacker-controlled name or a
 * tampered reference cannot escape the base directory (Requirement 33.5).
 */
@Component
public class FilesystemAttachmentStorage implements AttachmentStorage {

    private static final Logger log = LoggerFactory.getLogger(FilesystemAttachmentStorage.class);

    private final Path baseDirectory;

    public FilesystemAttachmentStorage(AttachmentProperties properties) {
        this.baseDirectory = Paths.get(properties.baseDirectory()).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file) {
        String reference = UUID.randomUUID().toString();
        Path target = resolve(reference);
        try {
            Files.createDirectories(baseDirectory);
            try (InputStream bytes = file.getInputStream()) {
                Files.copy(bytes, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("Failed to write attachment {} under {}", reference, baseDirectory, e);
            throw new BusinessException(
                    "Unable to store the uploaded file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return reference;
    }

    @Override
    public Resource load(String storageReference) {
        Path source = resolve(storageReference);
        if (!Files.isRegularFile(source)) {
            log.error("Attachment {} has no file under {}", storageReference, baseDirectory);
            throw new BusinessException("Attachment not found", HttpStatus.NOT_FOUND);
        }
        return new FileSystemResource(source);
    }

    @Override
    public void delete(String storageReference) {
        try {
            Files.deleteIfExists(resolve(storageReference));
        } catch (IOException e) {
            log.warn("Failed to delete attachment file {}", storageReference, e);
        }
    }

    /**
     * Resolves a storage reference against the base directory. The reference must be a UUID, which
     * rules out separators, {@code ..} segments and absolute paths by construction.
     */
    private Path resolve(String storageReference) {
        UUID key;
        try {
            key = UUID.fromString(storageReference);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException("Attachment not found", HttpStatus.NOT_FOUND);
        }
        return baseDirectory.resolve(key.toString()).normalize();
    }
}
