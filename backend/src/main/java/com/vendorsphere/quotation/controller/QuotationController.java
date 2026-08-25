package com.vendorsphere.quotation.controller;

import com.vendorsphere.common.attachment.AttachmentOwnerType;
import com.vendorsphere.common.attachment.AttachmentResponse;
import com.vendorsphere.common.attachment.AttachmentService;
import com.vendorsphere.common.dto.ApiResponse;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.quotation.dto.ComparisonResponse;
import com.vendorsphere.quotation.dto.QuotationResponse;
import com.vendorsphere.quotation.dto.QuotationSelectRequest;
import com.vendorsphere.quotation.dto.QuotationSubmitRequest;
import com.vendorsphere.quotation.service.EvaluationCriteriaWeightService;
import com.vendorsphere.quotation.service.QuotationService;
import com.vendorsphere.quotation.service.SelectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quotation, comparison, evaluation and selection endpoints (Requirements 12 through 17).
 *
 * <p>Submission is vendor work; comparison and evaluation are procurement work. The comparison
 * endpoint's grant deliberately excludes VENDOR - a vendor asking for the comparison is answered 403
 * at this boundary before any service code runs (Requirement 14.4). Criteria weights are configured
 * by the PROCUREMENT_MANAGER (Requirement 30.5).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Quotations")
public class QuotationController {

    private final QuotationService quotationService;
    private final SelectionService selectionService;
    private final EvaluationCriteriaWeightService weightsService;
    private final AttachmentService attachmentService;

    public QuotationController(
            QuotationService quotationService,
            SelectionService selectionService,
            EvaluationCriteriaWeightService weightsService,
            AttachmentService attachmentService
    ) {
        this.quotationService = quotationService;
        this.selectionService = selectionService;
        this.weightsService = weightsService;
        this.attachmentService = attachmentService;
    }

    /** Requirement 12.1: a linked, invited vendor submits into an OPEN window. */
    @PostMapping("/rfqs/{rfqId}/quotations")
    @PreAuthorize("hasRole('VENDOR')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit or revise the linked vendor's quotation for an open RFQ")
    public ApiResponse<QuotationResponse> submit(
            @PathVariable UUID rfqId, @Valid @RequestBody QuotationSubmitRequest request) {
        return ApiResponse.ok("Quotation submitted", quotationService.submit(rfqId, request));
    }

    /** Requirement 14.3: a vendor user lists only its own quotations here. */
    @GetMapping("/rfqs/{rfqId}/quotations")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List an RFQ's quotations (vendor users see only their own)")
    public ApiResponse<List<QuotationResponse>> listForRfq(@PathVariable UUID rfqId) {
        return ApiResponse.ok(quotationService.listForRfq(rfqId));
    }

    @GetMapping("/quotations/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get one quotation (own for vendors; redacted internally while OPEN)")
    public ApiResponse<QuotationResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(quotationService.get(id));
    }

    @PostMapping(value = "/quotations/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('VENDOR')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Attach a supporting document to the vendor's quotation")
    public ApiResponse<AttachmentResponse> uploadDocument(
            @PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok("Document uploaded", attachmentService.upload(
                AttachmentOwnerType.QUOTATION, id, file));
    }

    /** Requirement 14.4: no VENDOR in the grant - the boundary itself answers 403. */
    @GetMapping("/rfqs/{rfqId}/comparison")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "The normalized side-by-side comparison of a closed RFQ")
    public ApiResponse<ComparisonResponse> compare(@PathVariable UUID rfqId) {
        return ApiResponse.ok(quotationService.compare(rfqId));
    }

    @PostMapping("/rfqs/{rfqId}/evaluate")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Score the RFQ's quotations with the organization's criteria weights")
    public ApiResponse<Void> evaluate(@PathVariable UUID rfqId) {
        quotationService.evaluate(rfqId);
        return ApiResponse.ok("Quotations evaluated", null);
    }

    @PostMapping("/rfqs/{rfqId}/select")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Award the RFQ to one quotation with a mandatory justification")
    public ApiResponse<Void> select(
            @PathVariable UUID rfqId, @Valid @RequestBody QuotationSelectRequest request) {
        selectionService.select(rfqId, request.quotationId(), request.justification());
        return ApiResponse.ok("Vendor selected", null);
    }

    /** Requirement 17.6: comments land on the quotation's evaluation record. */
    @PostMapping("/quotations/{id}/comments")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Record a procurement comment on a quotation's evaluation")
    public ApiResponse<Void> comment(@PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        selectionService.comment(id, body.get("comments"));
        return ApiResponse.ok("Comment recorded", null);
    }

    @org.springframework.web.bind.annotation.GetMapping("/evaluation-criteria-weights")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "The organization's criteria weights, or the platform defaults")
    public ApiResponse<Map<String, BigDecimal>> getWeights() {
        var current = weightsService.resolve();
        return ApiResponse.ok(Map.of(
                "price", current.price(),
                "delivery", current.delivery(),
                "performance", current.performance(),
                "warranty", current.warranty()));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/evaluation-criteria-weights")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Store the evaluation criteria weights; they must sum to 1.00")
    public ApiResponse<Map<String, BigDecimal>> saveWeights(
            @RequestBody Map<String, BigDecimal> body) {
        weightsService.save(body.get("price"), body.get("delivery"),
                body.get("performance"), body.get("warranty"));
        var current = weightsService.resolve();
        return ApiResponse.ok("Weights stored", Map.of(
                "price", current.price(),
                "delivery", current.delivery(),
                "performance", current.performance(),
                "warranty", current.warranty()));
    }
}
