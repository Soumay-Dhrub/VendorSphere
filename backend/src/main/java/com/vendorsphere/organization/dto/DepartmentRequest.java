package com.vendorsphere.organization.dto;

import com.vendorsphere.organization.entity.Department;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DepartmentRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 50) String code,
        UUID managerId
) {
}
