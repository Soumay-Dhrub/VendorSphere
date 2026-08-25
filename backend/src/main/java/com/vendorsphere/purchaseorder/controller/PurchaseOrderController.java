package com.vendorsphere.purchaseorder.controller;

import com.vendorsphere.common.dto.ApiResponse;
import com.vendorsphere.purchaseorder.dto.PurchaseOrderCancelRequest;
import com.vendorsphere.purchaseorder.dto.PurchaseOrderResponse;
import com.vendorsphere.purchaseorder.dto.PurchaseOrderUpdateRequest;
import com.vendorsphere.purchaseorder.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Purchase Orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping("/rfqs/{rfqId}/purchase-order")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generate a purchase order from the RFQ's vendor selection")
    public ApiResponse<PurchaseOrderResponse> generate(@PathVariable UUID rfqId) {
        return ApiResponse.ok("Purchase order generated", purchaseOrderService.generate(rfqId));
    }

    @GetMapping("/purchase-orders")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List purchase orders (vendor users see only their own, no drafts)")
    public ApiResponse<List<PurchaseOrderResponse>> list() {
        return ApiResponse.ok(purchaseOrderService.list());
    }

    @GetMapping("/purchase-orders/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a purchase order with items and delivery figures")
    public ApiResponse<PurchaseOrderResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(purchaseOrderService.get(id));
    }

    @PutMapping("/purchase-orders/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Edit the header of a draft purchase order")
    public ApiResponse<PurchaseOrderResponse> update(
            @PathVariable UUID id, @Valid @RequestBody PurchaseOrderUpdateRequest request) {
        return ApiResponse.ok(purchaseOrderService.update(id, request));
    }

    @PostMapping("/purchase-orders/{id}/issue")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    @Operation(summary = "Issue a draft purchase order to the vendor")
    public ApiResponse<PurchaseOrderResponse> issue(@PathVariable UUID id) {
        return ApiResponse.ok("Purchase order issued", purchaseOrderService.issue(id));
    }

    @PostMapping("/purchase-orders/{id}/acknowledge")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Acknowledge an issued purchase order as the linked vendor")
    public ApiResponse<PurchaseOrderResponse> acknowledge(@PathVariable UUID id) {
        return ApiResponse.ok("Purchase order acknowledged", purchaseOrderService.acknowledge(id));
    }

    @PostMapping("/purchase-orders/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Close a fully delivered purchase order")
    public ApiResponse<PurchaseOrderResponse> close(@PathVariable UUID id) {
        return ApiResponse.ok("Purchase order closed", purchaseOrderService.close(id));
    }

    @PostMapping("/purchase-orders/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Cancel a purchase order with a mandatory reason")
    public ApiResponse<PurchaseOrderResponse> cancel(
            @PathVariable UUID id,
            @RequestBody(required = false) PurchaseOrderCancelRequest request) {
        return ApiResponse.ok("Purchase order cancelled", purchaseOrderService.cancel(id, request));
    }
}
