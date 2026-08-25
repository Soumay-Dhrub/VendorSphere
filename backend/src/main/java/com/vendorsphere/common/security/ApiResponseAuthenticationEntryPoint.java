package com.vendorsphere.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

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
