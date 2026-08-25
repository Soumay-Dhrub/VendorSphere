package com.vendorsphere.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

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
