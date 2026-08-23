package com.vendorsphere.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * The filter-chain counterpart of {@code GlobalExceptionHandler}'s {@code AccessDeniedException}
 * handler: an authenticated caller refused outside a handler method gets the same 403 and the same
 * pinned {@code Access denied} wording in the same envelope.
 *
 * <p>A refusal raised inside a controller method — {@code @PreAuthorize}, or a vendor-scoped service
 * check — is resolved by the {@code DispatcherServlet} and never reaches here. This covers the rest.
 */
@Component
public class ApiResponseAccessDeniedHandler implements AccessDeniedHandler {

    static final String MESSAGE = "Access denied";

    private final ApiResponseErrorWriter errorWriter;

    public ApiResponseAccessDeniedHandler(ApiResponseErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        errorWriter.write(response, HttpStatus.FORBIDDEN, MESSAGE);
    }
}
