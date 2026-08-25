package com.vendorsphere.common.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vendorsphere.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public static final List<String> DEFAULT_ALLOWED_ORIGINS = List.of("http://localhost:3000");

    public CorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream()
                        .filter(origin -> origin != null && !origin.isBlank())
                        .map(String::trim)
                        .toList();

        if (allowedOrigins.contains("*")) {
            throw new IllegalArgumentException(
                    "vendorsphere.cors.allowed-origins must not contain \"*\": a wildcard origin "
                            + "combined with allowCredentials=true would let any site issue "
                            + "credentialed requests. List the origins explicitly, or use a "
                            + "subdomain pattern such as https://*.example.com.");
        }

        if (allowedOrigins.isEmpty()) {
            allowedOrigins = DEFAULT_ALLOWED_ORIGINS;
        }
    }
}
