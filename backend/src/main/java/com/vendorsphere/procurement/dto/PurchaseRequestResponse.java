package com.vendorsphere.procurement.dto;

import com.vendorsphere.procurement.PurchaseRequestStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

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
