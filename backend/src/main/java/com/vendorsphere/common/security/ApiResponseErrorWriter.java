package com.vendorsphere.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendorsphere.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiResponseErrorWriter {

    private final ObjectMapper objectMapper;

    public ApiResponseErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(message));
    }
}
