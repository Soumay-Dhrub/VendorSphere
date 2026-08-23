package com.vendorsphere.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the local filesystem attachment store (Requirement 33.7).
 *
 * <p>Bound from {@code vendorsphere.attachments} in {@code application.yml}. The base directory is
 * the only root under which uploaded bytes are ever written or read; storage references are random
 * UUIDs resolved against it, so no caller-supplied text takes part in path resolution.
 */
@ConfigurationProperties(prefix = "vendorsphere.attachments")
public record AttachmentProperties(String baseDirectory) {

    /** Used when the property is absent so a misconfigured environment cannot escape the store. */
    private static final String DEFAULT_BASE_DIRECTORY = "./data/attachments";

    public AttachmentProperties {
        if (baseDirectory == null || baseDirectory.isBlank()) {
            baseDirectory = DEFAULT_BASE_DIRECTORY;
        }
    }
}
