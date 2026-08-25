package com.vendorsphere.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vendorsphere.attachments")
public record AttachmentProperties(String baseDirectory) {

    private static final String DEFAULT_BASE_DIRECTORY = "./data/attachments";

    public AttachmentProperties {
        if (baseDirectory == null || baseDirectory.isBlank()) {
            baseDirectory = DEFAULT_BASE_DIRECTORY;
        }
    }
}
