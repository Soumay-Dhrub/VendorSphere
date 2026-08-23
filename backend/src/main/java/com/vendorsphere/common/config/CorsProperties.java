package com.vendorsphere.common.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Browser origins allowed to call the API.
 *
 * <p>Bound from {@code vendorsphere.cors} in {@code application.yml}, which sources the value from
 * the {@code CORS_ALLOWED_ORIGINS} environment variable so a deployment can point the API at a
 * frontend served from any host or port. Entries are matched as patterns (see
 * {@code SecurityConfig#corsConfigurationSource()}), so {@code https://*.example.com} is a legal
 * value.
 *
 * <p>The default is the single local development origin. A bare {@code "*"} is rejected: the CORS
 * configuration sends {@code Access-Control-Allow-Credentials: true}, and a wildcard there would
 * let any site issue credentialed cross-origin requests against the API. Failing at startup is
 * preferable to silently serving a permissive policy.
 */
@ConfigurationProperties(prefix = "vendorsphere.cors")
public record CorsProperties(List<String> allowedOrigins) {

    /** Used when the property is absent or blank, matching the documented local setup. */
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
