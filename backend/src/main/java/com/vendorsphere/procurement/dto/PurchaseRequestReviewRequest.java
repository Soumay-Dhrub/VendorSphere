package com.vendorsphere.procurement.dto;

/**
 * Review decision payload (Requirements 8.5 through 8.7).
 *
 * <p>One record serves both decisions. On approval the comments are optional review notes
 * (Requirement 8.5); on rejection the same field carries the reason, which the service requires -
 * blank counts as missing, answered with 400 {@code Rejection reason is required} (Requirement 8.6).
 */
public record PurchaseRequestReviewRequest(
        String comments
) {
}
