package com.vendorsphere.vendor.controller;

import com.vendorsphere.common.dto.ApiResponse;
import com.vendorsphere.vendor.dto.VendorCategoryRequest;
import com.vendorsphere.vendor.dto.VendorCategoryResponse;
import com.vendorsphere.vendor.service.VendorCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Vendor category endpoints (Requirement 4.4, API surface).
 *
 * <p>Category management is procurement-officer work (Requirement 30.4) and ADMIN inherits it
 * (Requirement 30.3). The list is read by name ascending with no paging, because a taxonomy is a
 * handful of rows a form renders whole, not a dataset.
 */
@RestController
@RequestMapping("/api/v1/vendor-categories")
@Tag(name = "Vendor Categories")
public class VendorCategoryController {

    private final VendorCategoryService vendorCategoryService;

    public VendorCategoryController(VendorCategoryService vendorCategoryService) {
        this.vendorCategoryService = vendorCategoryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "List the current organization's vendor categories by name")
    public ApiResponse<List<VendorCategoryResponse>> list() {
        return ApiResponse.ok(vendorCategoryService.list());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a vendor category")
    public ApiResponse<VendorCategoryResponse> create(
            @Valid @RequestBody VendorCategoryRequest request) {
        return ApiResponse.ok("Vendor category created", vendorCategoryService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    @Operation(summary = "Update a vendor category")
    public ApiResponse<VendorCategoryResponse> update(
            @PathVariable UUID id, @Valid @RequestBody VendorCategoryRequest request) {
        return ApiResponse.ok(vendorCategoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    @Operation(summary = "Delete an unreferenced vendor category")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        vendorCategoryService.delete(id);
        return ApiResponse.ok("Vendor category deleted", null);
    }
}
