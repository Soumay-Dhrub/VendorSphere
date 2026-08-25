package com.vendorsphere.delivery.controller;

import com.vendorsphere.common.dto.ApiResponse;
import com.vendorsphere.delivery.dto.DeliveryProgressResponse;
import com.vendorsphere.delivery.dto.DeliveryRecordRequest;
import com.vendorsphere.delivery.service.DeliveryService;
import com.vendorsphere.vendor.service.VendorAccessGuard;
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
@Tag(name = "Deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final VendorAccessGuard vendorAccessGuard;

    public DeliveryController(DeliveryService deliveryService, VendorAccessGuard vendorAccessGuard) {
        this.deliveryService = deliveryService;
        this.vendorAccessGuard = vendorAccessGuard;
    }

    @PostMapping("/purchase-orders/{poId}/deliveries")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'VENDOR')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a goods receipt against an in-flight purchase order")
    public ApiResponse<Void> record(
            @PathVariable UUID poId, @Valid @RequestBody DeliveryRecordRequest request) {
        deliveryService.record(poId, request);
        return ApiResponse.ok("Delivery recorded", null);
    }

    @GetMapping("/purchase-orders/{poId}/delivery-progress")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Per-line ordered, received, damaged, rejected and outstanding quantities")
    public ApiResponse<DeliveryProgressResponse> progress(@PathVariable UUID poId) {
        return ApiResponse.ok(new DeliveryProgressResponse(
                poId, deliveryService.progress(poId)));
    }
}
