package com.vendorsphere.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Requirement 30.2: a request that carries no JWT, a malformed one or an expired one is answered
 * with 401 and an {@code ApiResponse} whose {@code success} is false.
 *
 * <p>{@code JwtAuthenticationFilter} leaves the security context empty rather than throwing, so such
 * a request reaches the authorization filter as anonymous. Without an entry point Spring Security
 * falls back to {@code Http403ForbiddenEntryPoint} and answers 403 — the status that belongs to an
 * authenticated caller lacking a role, not to an unauthenticated one.
 */
@Component
public class ApiResponseAuthenticationEntryPoint implements AuthenticationEntryPoint {

    static final String MESSAGE = "Authentication required";

    private final ApiResponseErrorWriter errorWriter;

    public ApiResponseAuthenticationEntryPoint(ApiResponseErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        errorWriter.write(response, HttpStatus.UNAUTHORIZED, MESSAGE);
    }
}
