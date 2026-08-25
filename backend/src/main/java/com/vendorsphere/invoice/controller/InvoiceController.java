package com.vendorsphere.invoice.controller;

import com.vendorsphere.common.dto.ApiResponse;
import com.vendorsphere.invoice.dto.InvoiceResponse;
import com.vendorsphere.invoice.dto.InvoiceSubmitRequest;
import com.vendorsphere.invoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Invoice endpoints (Requirements 22 through 24). Review verbs are FINANCE/ADMIN only, so a VENDOR
 * request is answered 403 at the boundary (Requirement 24.11).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/purchase-orders/{poId}/invoices")
    @PreAuthorize("hasAnyRole('VENDOR', 'FINANCE', 'PROCUREMENT_OFFICER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit an invoice against a billable purchase order")
    public ApiResponse<InvoiceResponse> submit(
            @PathVariable UUID poId, @Valid @RequestBody InvoiceSubmitRequest request) {
        return ApiResponse.ok("Invoice submitted", invoiceService.submit(poId, request));
    }

    @GetMapping("/invoices")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List invoices (vendor users see only their own)")
    public ApiResponse<List<InvoiceResponse>> list() {
        return ApiResponse.ok(invoiceService.list());
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get one invoice")
    public ApiResponse<InvoiceResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(invoiceService.get(id));
    }

    @GetMapping("/invoices/{id}/match")
    @PreAuthorize("hasAnyRole('FINANCE', 'PROCUREMENT_MANAGER', 'ADMIN')")
    @Operation(summary = "The three-way match result: status, findings, per-item figures")
    public ApiResponse<InvoiceResponse.MatchResult> matchResult(@PathVariable UUID id) {
        return ApiResponse.ok(invoiceService.matchResult(id));
    }

    @PostMapping("/invoices/{id}/review")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    @Operation(summary = "Approve or reject an invoice (approve blocked on unresolved findings)")
    public ApiResponse<Void> review(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        boolean approve = Boolean.TRUE.equals(body.get("approve"));
        String comments = body.get("comments") == null ? null : body.get("comments").toString();
        invoiceService.review(id, approve, comments);
        return ApiResponse.ok(approve ? "Invoice approved" : "Invoice rejected", null);
    }

    @PostMapping("/invoices/{id}/match-findings/{findingId}/override")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    @Operation(summary = "Override a match finding with a mandatory justification")
    public ApiResponse<Void> overrideFinding(
            @PathVariable UUID id, @PathVariable UUID findingId,
            @RequestBody Map<String, String> body) {
        invoiceService.overrideFinding(id, findingId, body.get("justification"));
        return ApiResponse.ok("Match finding overridden", null);
    }
}
