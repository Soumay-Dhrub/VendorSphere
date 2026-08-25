package com.vendorsphere.rfq.controller;

import com.vendorsphere.common.attachment.AttachmentOwnerType;
import com.vendorsphere.common.attachment.AttachmentResponse;
import com.vendorsphere.common.attachment.AttachmentService;
import com.vendorsphere.common.dto.ApiResponse;
import com.vendorsphere.common.dto.PageResponse;
import com.vendorsphere.common.util.PageSupport;
import com.vendorsphere.rfq.RfqStatus;
import com.vendorsphere.rfq.dto.RfqCancelRequest;
import com.vendorsphere.rfq.dto.RfqCreateRequest;
import com.vendorsphere.rfq.dto.RfqItemRequest;
import com.vendorsphere.rfq.dto.RfqResponse;
import com.vendorsphere.rfq.dto.RfqSearchCriteria;
import com.vendorsphere.rfq.dto.RfqUpdateRequest;
import com.vendorsphere.rfq.service.RfqService;
import com.vendorsphere.rfq.service.RfqVendorService;
import com.vendorsphere.vendor.service.VendorAccessGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rfqs")
@Tag(name = "RFQs")
public class RfqController {

    private final RfqService rfqService;
    private final RfqVendorService rfqVendorService;
    private final AttachmentService attachmentService;
    private final VendorAccessGuard vendorAccessGuard;

    public RfqController(
            RfqService rfqService,
            RfqVendorService rfqVendorService,
            AttachmentService attachmentService,
            VendorAccessGuard vendorAccessGuard
    ) {
        this.rfqService = rfqService;
        this.rfqVendorService = rfqVendorService;
        this.attachmentService = attachmentService;
        this.vendorAccessGuard = vendorAccessGuard;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "List RFQs of the current organization")
    public ApiResponse<PageResponse<RfqResponse>> list(
            @RequestParam(required = false) RfqStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ApiResponse.ok(rfqService.search(
                new RfqSearchCriteria(status),
                PageSupport.pageable(page, size, sort, direction, RfqService.SORTABLE)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an RFQ from an approved purchase request")
    public ApiResponse<RfqResponse> create(@Valid @RequestBody RfqCreateRequest request) {
        return ApiResponse.ok("RFQ created", rfqService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Get an RFQ with items and invitations")
    public ApiResponse<RfqResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(rfqService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Update the header of a draft RFQ")
    public ApiResponse<RfqResponse> update(
            @PathVariable UUID id, @Valid @RequestBody RfqUpdateRequest request) {
        return ApiResponse.ok(rfqService.update(id, request));
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Add an item to a draft RFQ")
    public ApiResponse<RfqResponse> addItem(
            @PathVariable UUID id, @Valid @RequestBody RfqItemRequest request) {
        return ApiResponse.ok("Item added", rfqService.addItem(id, request));
    }

    @PutMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Update an item of a draft RFQ")
    public ApiResponse<RfqResponse> updateItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @Valid @RequestBody RfqItemRequest request
    ) {
        return ApiResponse.ok(rfqService.updateItem(id, itemId, request));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Remove an item from a draft RFQ")
    public ApiResponse<RfqResponse> removeItem(@PathVariable UUID id, @PathVariable UUID itemId) {
        return ApiResponse.ok(rfqService.removeItem(id, itemId));
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Attach a document to a draft RFQ")
    public ApiResponse<AttachmentResponse> uploadDocument(
            @PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok("Document uploaded", attachmentService.upload(
                AttachmentOwnerType.RFQ, id, file));
    }

    @PostMapping("/{id}/vendors")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Invite active vendors to the RFQ (all-or-nothing)")
    public ApiResponse<List<RfqResponse.RfqVendorResponse>> invite(
            @PathVariable UUID id, @Valid @RequestBody com.vendorsphere.rfq.dto.RfqInviteRequest request) {
        return ApiResponse.ok("Vendors invited", rfqVendorService.invite(id, request));
    }

    @PostMapping("/{id}/open")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    @Operation(summary = "Open a draft RFQ for bidding")
    public ApiResponse<RfqResponse> open(@PathVariable UUID id) {
        return ApiResponse.ok("RFQ opened", rfqService.open(id));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    @Operation(summary = "Close an open RFQ")
    public ApiResponse<RfqResponse> close(@PathVariable UUID id) {
        return ApiResponse.ok("RFQ closed", rfqService.close(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Cancel an RFQ with a mandatory reason")
    public ApiResponse<RfqResponse> cancel(
            @PathVariable UUID id, @RequestBody(required = false) RfqCancelRequest request) {
        return ApiResponse.ok("RFQ cancelled", rfqService.cancel(id, request));
    }

    // ----- vendor portal reads -----

    @GetMapping("/vendor")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "List the linked vendor's invited RFQs")
    public ApiResponse<List<RfqResponse>> listForVendor() {
        UUID organizationId = com.vendorsphere.common.security.SecurityUtils.getCurrentOrganizationId();
        UUID vendorId = vendorAccessGuard.currentVendorId()
                .orElseThrow(() -> new com.vendorsphere.common.exception.BusinessException(
                        "Access denied", HttpStatus.FORBIDDEN));
        return ApiResponse.ok(rfqVendorService.listForVendor(organizationId, vendorId).stream()
                .map(rfq -> RfqResponse.from(rfq, null, null))
                .toList());
    }

    @GetMapping("/vendor/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Read an invited RFQ as the linked vendor, marking it viewed")
    public ApiResponse<RfqResponse> getForVendor(@PathVariable UUID id) {
        UUID organizationId = com.vendorsphere.common.security.SecurityUtils.getCurrentOrganizationId();
        UUID vendorId = vendorAccessGuard.currentVendorId()
                .orElseThrow(() -> new com.vendorsphere.common.exception.BusinessException(
                        "Access denied", HttpStatus.FORBIDDEN));
        return ApiResponse.ok(RfqResponse.from(
                rfqVendorService.getForVendor(organizationId, vendorId, id), null, null));
    }
}
