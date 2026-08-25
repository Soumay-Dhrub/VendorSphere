package com.vendorsphere.procurement.dto;

import com.vendorsphere.procurement.PurchaseRequestStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One purchase request as returned by a create, update, review, detail or list read.
 *
 * <p>Carries every figure Requirement 8.9 asks a requester-facing read to report - status, reviewer
 * name when recorded, review timestamp and notes when present and the identifiers of derived RFQs -
 * alongside the authoring fields. {@code items} is ordered by authoring order; list reads supply it
 * batched for the whole page so per-row cost stays fixed (Requirement 31.2).
 */
public record PurchaseRequestResponse(
        UUID id,
        String requestNumber,
        String title,
        UUID departmentId,
        String departmentName,
        UUID requesterId,
        String businessJustification,
        LocalDate requiredDate,
        com.vendorsphere.procurement.Priority priority,
        BigDecimal estimatedBudget,
        PurchaseRequestStatus status,
        UUID reviewedById,
        String reviewedByName,
        Instant reviewedAt,
        String reviewNotes,
        java.util.List<PurchaseRequestItemResponse> items,
        java.util.List<UUID> rfqIds,
        Instant createdAt,
        Instant updatedAt
) {

    /** One line item of a purchase request (Requirement 7.4). */
    public record PurchaseRequestItemResponse(
            UUID id,
            String itemName,
            BigDecimal quantity,
            String unit,
            String specification,
            int sortOrder
    ) {

        public static PurchaseRequestItemResponse from(
                com.vendorsphere.procurement.entity.PurchaseRequestItem item) {
            return new PurchaseRequestItemResponse(
                    item.getId(),
                    item.getItemName(),
                    item.getQuantity(),
                    item.getUnit(),
                    item.getSpecification(),
                    item.getSortOrder());
        }
    }

    /**
     * Projects a stored request; the caller supplies the queried figures - items, reviewer name and
     * derived RFQ identifiers - because they are resolved outside this record.
     */
    public static PurchaseRequestResponse from(
            com.vendorsphere.procurement.entity.PurchaseRequest request,
            java.util.List<PurchaseRequestItemResponse> items,
            java.util.List<UUID> rfqIds,
            String reviewedByName) {
        return new PurchaseRequestResponse(
                request.getId(),
                request.getRequestNumber(),
                request.getTitle(),
                request.getDepartment().getId(),
                request.getDepartment().getName(),
                request.getRequester().getId(),
                request.getBusinessJustification(),
                request.getRequiredDate(),
                request.getPriority(),
                request.getEstimatedBudget(),
                request.getStatus(),
                request.getReviewedBy() != null ? request.getReviewedBy().getId() : null,
                reviewedByName,
                request.getReviewedAt(),
                request.getReviewNotes(),
                items,
                rfqIds,
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
