package com.vendorsphere.user.dto;

import com.vendorsphere.user.entity.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID organizationId,
        UUID departmentId,
        String email,
        String firstName,
        String lastName,
        String phone,
        boolean active,
        List<String> roles,
        Instant lastLoginAt,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getOrganization().getId(),
                user.getDepartment() != null ? user.getDepartment().getId() : null,
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.isActive(),
                user.getRoles().stream().map(role -> role.getName()).sorted().toList(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
