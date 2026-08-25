package com.vendorsphere.rfq.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Invitation payload (Requirement 10.1): one or more vendor identifiers, validated all-or-nothing -
 * one inactive or already-invited vendor rejects the whole request.
 */
public record RfqInviteRequest(
        @NotEmpty List<UUID> vendorIds
) {
}
