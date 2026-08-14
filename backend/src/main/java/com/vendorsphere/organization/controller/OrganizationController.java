package com.vendorsphere.organization.controller;

import com.vendorsphere.common.dto.ApiResponse;
import com.vendorsphere.organization.dto.DepartmentRequest;
import com.vendorsphere.organization.dto.DepartmentResponse;
import com.vendorsphere.organization.dto.OrganizationResponse;
import com.vendorsphere.organization.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Organization")
public class OrganizationController {

    private final DepartmentService departmentService;

    public OrganizationController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("/organizations/me")
    @Operation(summary = "Get current user's organization")
    public ApiResponse<OrganizationResponse> getCurrentOrganization() {
        return ApiResponse.ok(departmentService.getCurrentOrganization());
    }

    @GetMapping("/departments")
    @Operation(summary = "List departments in the current organization")
    public ApiResponse<List<DepartmentResponse>> listDepartments(
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return ApiResponse.ok(departmentService.listDepartments(activeOnly));
    }

    @GetMapping("/departments/{id}")
    @Operation(summary = "Get department by ID")
    public ApiResponse<DepartmentResponse> getDepartment(@PathVariable UUID id) {
        return ApiResponse.ok(departmentService.getDepartment(id));
    }

    @PostMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a department")
    public ApiResponse<DepartmentResponse> createDepartment(
            @Valid @RequestBody DepartmentRequest request
    ) {
        return ApiResponse.ok("Department created", departmentService.createDepartment(request));
    }

    @PutMapping("/departments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a department")
    public ApiResponse<DepartmentResponse> updateDepartment(
            @PathVariable UUID id,
            @Valid @RequestBody DepartmentRequest request
    ) {
        return ApiResponse.ok(departmentService.updateDepartment(id, request));
    }

    @PatchMapping("/departments/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Disable a department")
    public ApiResponse<DepartmentResponse> disableDepartment(@PathVariable UUID id) {
        return ApiResponse.ok(departmentService.disableDepartment(id));
    }
}
