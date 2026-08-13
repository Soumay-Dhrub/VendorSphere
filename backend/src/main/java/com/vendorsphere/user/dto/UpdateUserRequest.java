package com.vendorsphere.user.dto;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UpdateUserRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 30) String phone,
        UUID departmentId,
        Boolean active,
        List<@Size(max = 50) String> roles
) {
}
