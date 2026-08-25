package com.vendorsphere.procurement.controller;

import com.vendorsphere.common.attachment.AttachmentOwnerType;
import com.vendorsphere.common.attachment.AttachmentResponse;
import com.vendorsphere.common.attachment.AttachmentService;
import com.vendorsphere.common.dto.ApiResponse;
import com.vendorsphere.common.dto.PageResponse;
import com.vendorsphere.common.util.PageSupport;
import com.vendorsphere.procurement.PurchaseRequestStatus;
import com.vendorsphere.procurement.dto.PurchaseRequestHeaderRequest;
import com.vendorsphere.procurement.dto.PurchaseRequestItemRequest;
import com.vendorsphere.procurement.dto.PurchaseRequestResponse;
import com.vendorsphere.procurement.dto.PurchaseRequestReviewRequest;
import com.vendorsphere.procurement.dto.PurchaseRequestSearchCriteria;
import com.vendorsphere.procurement.service.PurchaseRequestService;
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
@RequestMapping("/api/v1/purchase-requests")
@Tag(name = "Purchase Requests")
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;
    private final AttachmentService attachmentService;

    public PurchaseRequestController(
            PurchaseRequestService purchaseRequestService,
            AttachmentService attachmentService
    ) {
        this.purchaseRequestService = purchaseRequestService;
        this.attachmentService = attachmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER', 'REQUESTER')")
    @Operation(summary = "List purchase requests (REQUESTER sees only its own)")
    public ApiResponse<PageResponse<PurchaseRequestResponse>> list(
            @RequestParam(required = false) PurchaseRequestStatus status,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ApiResponse.ok(purchaseRequestService.search(
                new PurchaseRequestSearchCriteria(status, departmentId),
                PageSupport.pageable(
                        page, size, sort, direction, PurchaseRequestService.SORTABLE)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER', 'REQUESTER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a draft purchase request")
    public ApiResponse<PurchaseRequestResponse> create(
            @Valid @RequestBody PurchaseRequestHeaderRequest request) {
        return ApiResponse.ok("Purchase request created",
                purchaseRequestService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER', 'REQUESTER')")
    @Operation(summary = "Get a purchase request with items, review data and derived RFQs")
    public ApiResponse<PurchaseRequestResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(purchaseRequestService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER', 'REQUESTER')")
    @Operation(summary = "Update a draft purchase request's header")
    public ApiResponse<PurchaseRequestResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody PurchaseRequestHeaderRequest request
    ) {
        return ApiResponse.ok(purchaseRequestService.update(id, request));
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER', 'REQUESTER')")
    @Operation(summary = "Add an item to a draft purchase request")
    public ApiResponse<PurchaseRequestResponse> addItem(
            @PathVariable UUID id,
            @Valid @RequestBody PurchaseRequestItemRequest request
    ) {
        return ApiResponse.ok("Item added", purchaseRequestService.addItem(id, request));
    }

    @PutMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER', 'REQUESTER')")
    @Operation(summary = "Update an item of a draft purchase request")
    public ApiResponse<PurchaseRequestResponse> updateItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @Valid @RequestBody PurchaseRequestItemRequest request
    ) {
        return ApiResponse.ok(purchaseRequestService.updateItem(id, itemId, request));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER', 'REQUESTER')")
    @Operation(summary = "Remove an item from a draft purchase request")
    public ApiResponse<PurchaseRequestResponse> removeItem(
            @PathVariable UUID id, @PathVariable UUID itemId) {
        return ApiResponse.ok(purchaseRequestService.removeItem(id, itemId));
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'REQUESTER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Attach a supporting file to a draft purchase request")
    public ApiResponse<AttachmentResponse> uploadAttachment(
            @PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok("Attachment uploaded", attachmentService.upload(
                AttachmentOwnerType.PURCHASE_REQUEST, id, file));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER', 'REQUESTER')")
    @Operation(summary = "Submit a draft purchase request for review")
    public ApiResponse<PurchaseRequestResponse> submit(@PathVariable UUID id) {
        return ApiResponse.ok(purchaseRequestService.submit(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Approve a submitted purchase request")
    public ApiResponse<PurchaseRequestResponse> approve(
            @PathVariable UUID id, @RequestBody(required = false) PurchaseRequestReviewRequest request) {
        return ApiResponse.ok("Purchase request approved",
                purchaseRequestService.approve(id, request));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Reject a submitted purchase request with a mandatory reason")
    public ApiResponse<PurchaseRequestResponse> reject(
            @PathVariable UUID id, @RequestBody(required = false) PurchaseRequestReviewRequest request) {
        return ApiResponse.ok("Purchase request rejected",
                purchaseRequestService.reject(id, request));
    }
}
