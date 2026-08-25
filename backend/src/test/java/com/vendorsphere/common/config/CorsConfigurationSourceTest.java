package com.vendorsphere.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

class CorsConfigurationSourceTest {

    private static final String LOCAL_FRONTEND = "http://localhost:3000";

    @Test
    void defaultAllowsOnlyTheLocalFrontendOrigin() {
        CorsConfiguration config = corsConfigurationFor(new CorsProperties(null));

        assertThat(config.getAllowedOriginPatterns()).containsExactly(LOCAL_FRONTEND);
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.checkOrigin(LOCAL_FRONTEND)).isEqualTo(LOCAL_FRONTEND);
        assertThat(config.checkOrigin("http://evil.example.com")).isNull();
    }

    @Test
    void configuredOriginsReplaceTheDefault() {
        CorsConfiguration config = corsConfigurationFor(
                new CorsProperties(List.of("http://localhost:4200", "https://app.example.com")));

        assertThat(config.checkOrigin("http://localhost:4200")).isEqualTo("http://localhost:4200");
        assertThat(config.checkOrigin("https://app.example.com")).isEqualTo("https://app.example.com");
        // The default is replaced, not added to.
        assertThat(config.checkOrigin(LOCAL_FRONTEND)).isNull();
        assertThat(config.getAllowCredentials()).isTrue();
    }

    @Test
    void subdomainPatternIsMatchedWhileCredentialsStayEnabled() {
        CorsConfiguration config =
                corsConfigurationFor(new CorsProperties(List.of("https://*.example.com")));

        assertThat(config.checkOrigin("https://tenant-a.example.com"))
                .isEqualTo("https://tenant-a.example.com");
        assertThat(config.checkOrigin("https://example.attacker.com")).isNull();
        assertThat(config.getAllowCredentials()).isTrue();
    }

    @Test
    void blankAndMissingValuesFallBackToTheLocalFrontendRatherThanAllowingEverything() {
        assertThat(new CorsProperties(List.of()).allowedOrigins()).containsExactly(LOCAL_FRONTEND);
        assertThat(new CorsProperties(List.of("   ")).allowedOrigins()).containsExactly(LOCAL_FRONTEND);
    }

    @Test
    void wildcardOriginIsRejectedBecauseCredentialsAreAllowed() {
        assertThatThrownBy(() -> new CorsProperties(List.of("*")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not contain");
    }

    @Test
    void listBindsFromTheRelaxedEnvironmentVariableName() {
        new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                        new SystemEnvironmentPropertySource(
                                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                                Map.of("VENDORSPHERE_CORS_ALLOWEDORIGINS",
                                        "https://app.example.com,https://admin.example.com"))))
                .withUserConfiguration(CorsPropertiesConfiguration.class)
                .run(context -> assertThat(context.getBean(CorsProperties.class).allowedOrigins())
                        .containsExactly("https://app.example.com", "https://admin.example.com"));
    }

    private CorsConfiguration corsConfigurationFor(CorsProperties properties) {
        SecurityConfig securityConfig = new SecurityConfig(null, null, null, null, properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/vendors");
        CorsConfiguration config =
                securityConfig.corsConfigurationSource().getCorsConfiguration(request);
        assertThat(config).isNotNull();
        return config;
    }

    @Configuration
    @EnableConfigurationProperties(CorsProperties.class)
    static class CorsPropertiesConfiguration {
    }
}
