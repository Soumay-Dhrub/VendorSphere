package com.vendorsphere.organization.dto;

import com.vendorsphere.organization.entity.Department;

import java.time.Instant;
import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        UUID organizationId,
        String name,
        String code,
        UUID managerId,
        boolean active,
        Instant createdAt
) {

    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getOrganization().getId(),
                department.getName(),
                department.getCode(),
                department.getManager() != null ? department.getManager().getId() : null,
                department.isActive(),
                department.getCreatedAt()
        );
    }
}
