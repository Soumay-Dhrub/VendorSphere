package com.vendorsphere.vendor.controller;

import com.vendorsphere.common.dto.ApiResponse;
import com.vendorsphere.common.dto.PageResponse;
import com.vendorsphere.common.util.PageSupport;
import com.vendorsphere.vendor.VendorStatus;
import com.vendorsphere.vendor.dto.VendorContactRequest;
import com.vendorsphere.vendor.dto.VendorContactResponse;
import com.vendorsphere.vendor.dto.VendorDocumentRequest;
import com.vendorsphere.vendor.dto.VendorDocumentResponse;
import com.vendorsphere.vendor.dto.VendorPerformanceResponse;
import com.vendorsphere.vendor.dto.VendorRequest;
import com.vendorsphere.vendor.dto.VendorResponse;
import com.vendorsphere.vendor.dto.VendorSearchCriteria;
import com.vendorsphere.vendor.dto.VendorStatusChangeRequest;
import com.vendorsphere.vendor.dto.VendorStatusSnapshot;
import com.vendorsphere.vendor.service.VendorContactService;
import com.vendorsphere.vendor.service.VendorDocumentService;
import com.vendorsphere.vendor.service.VendorService;
import com.vendorsphere.vendor.service.VendorStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vendors")
@Tag(name = "Vendors")
public class VendorController {

    private final VendorService vendorService;
    private final VendorContactService vendorContactService;
    private final VendorDocumentService vendorDocumentService;
    private final VendorStatusService vendorStatusService;

    public VendorController(
            VendorService vendorService,
            VendorContactService vendorContactService,
            VendorDocumentService vendorDocumentService,
            VendorStatusService vendorStatusService
    ) {
        this.vendorService = vendorService;
        this.vendorContactService = vendorContactService;
        this.vendorDocumentService = vendorDocumentService;
        this.vendorStatusService = vendorStatusService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "List vendors of the current organization with optional filters")
    public ApiResponse<PageResponse<VendorResponse>> list(
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) VendorStatus status,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return ApiResponse.ok(vendorService.search(
                new VendorSearchCriteria(companyName, categoryId, status, minRating),
                PageSupport.pageable(page, size, sort, direction, VendorService.SORTABLE)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a vendor")
    public ApiResponse<VendorResponse> register(@Valid @RequestBody VendorRequest request) {
        return ApiResponse.ok("Vendor registered", vendorService.register(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER', 'VENDOR')")
    @Operation(summary = "Get a vendor profile with category, performance score and expiring documents")
    public ApiResponse<VendorResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(vendorService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'VENDOR')")
    @Operation(summary = "Update a vendor profile")
    public ApiResponse<VendorResponse> update(
            @PathVariable UUID id, @Valid @RequestBody VendorRequest request) {
        return ApiResponse.ok(vendorService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Change a vendor's lifecycle status")
    public ApiResponse<VendorStatusSnapshot> changeStatus(
            @PathVariable UUID id, @Valid @RequestBody VendorStatusChangeRequest request) {
        return ApiResponse.ok(vendorStatusService.changeStatus(id, request));
    }

    @GetMapping("/{id}/contacts")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    @Operation(summary = "List a vendor's contacts, primary first")
    public ApiResponse<List<VendorContactResponse>> listContacts(@PathVariable UUID id) {
        return ApiResponse.ok(vendorContactService.list(id));
    }

    @PostMapping("/{id}/contacts")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a contact to a vendor")
    public ApiResponse<VendorContactResponse> addContact(
            @PathVariable UUID id, @Valid @RequestBody VendorContactRequest request) {
        return ApiResponse.ok("Vendor contact added", vendorContactService.add(id, request));
    }

    @PutMapping("/{id}/contacts/{contactId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    @Operation(summary = "Update one of a vendor's contacts")
    public ApiResponse<VendorContactResponse> updateContact(
            @PathVariable UUID id,
            @PathVariable UUID contactId,
            @Valid @RequestBody VendorContactRequest request
    ) {
        return ApiResponse.ok(vendorContactService.update(id, contactId, request));
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    @Operation(summary = "Remove one of a vendor's contacts")
    public ApiResponse<Void> deleteContact(@PathVariable UUID id, @PathVariable UUID contactId) {
        vendorContactService.delete(id, contactId);
        return ApiResponse.ok("Vendor contact removed", null);
    }

    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'VENDOR')")
    @Operation(summary = "List a vendor's compliance documents with derived expiry states")
    public ApiResponse<List<VendorDocumentResponse>> listDocuments(@PathVariable UUID id) {
        return ApiResponse.ok(vendorDocumentService.list(id));
    }

    @PostMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'VENDOR')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a compliance document for a vendor")
    public ApiResponse<VendorDocumentResponse> uploadDocument(
            @PathVariable UUID id,
            @RequestPart("document") @Valid VendorDocumentRequest document,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.ok(
                "Vendor document uploaded", vendorDocumentService.upload(id, document, file));
    }

    @GetMapping("/{id}/performance")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Get a vendor's current performance score and rating")
    public ApiResponse<VendorPerformanceResponse> performance(@PathVariable UUID id) {
        return ApiResponse.ok(vendorService.performance(id));
    }
}
