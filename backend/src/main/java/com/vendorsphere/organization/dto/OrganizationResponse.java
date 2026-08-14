package com.vendorsphere.organization.dto;

import com.vendorsphere.organization.entity.Organization;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String slug,
        String address,
        String taxIdentifier,
        String currency,
        boolean active,
        Instant createdAt
) {

    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getAddress(),
                organization.getTaxIdentifier(),
                organization.getCurrency(),
                organization.isActive(),
                organization.getCreatedAt()
        );
    }
}
