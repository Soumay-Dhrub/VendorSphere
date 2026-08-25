package com.vendorsphere.payment.controller;

import com.vendorsphere.common.dto.ApiResponse;
import com.vendorsphere.payment.dto.OutstandingResponse;
import com.vendorsphere.payment.dto.PaymentRecordRequest;
import com.vendorsphere.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Payment endpoints (Requirement 25; API surface). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/invoices/{invoiceId}/payments")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a payment against an approved invoice")
    public ApiResponse<Void> record(
            @PathVariable UUID invoiceId, @Valid @RequestBody PaymentRecordRequest request) {
        paymentService.record(invoiceId, request);
        return ApiResponse.ok("Payment recorded", null);
    }

    @GetMapping("/payments/outstanding")
    @PreAuthorize("hasAnyRole('FINANCE', 'PROCUREMENT_MANAGER', 'ADMIN')")
    @Operation(summary = "Outstanding payables in total and by vendor")
    public ApiResponse<OutstandingResponse> outstanding() {
        return ApiResponse.ok(paymentService.outstanding());
    }
}
