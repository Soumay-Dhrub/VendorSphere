package com.vendorsphere.rfq.dto;

/**
 * Cancellation payload (Requirement 11.7): the reason is mandatory and becomes part of the audit
 * trail; a blank value is answered with 400 {@code Cancellation reason is required}.
 */
public record RfqCancelRequest(
        String reason
) {
}
