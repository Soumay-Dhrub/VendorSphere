package com.vendorsphere.rfq.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record RfqInviteRequest(
        @NotEmpty List<UUID> vendorIds
) {
}
