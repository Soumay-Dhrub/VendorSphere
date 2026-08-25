package com.vendorsphere.vendor.dto;

import com.vendorsphere.vendor.entity.VendorContact;

import java.time.Instant;
import java.util.UUID;

public record VendorContactResponse(
        UUID id,
        UUID vendorId,
        String name,
        String email,
        String phone,
        String designation,
        boolean primaryContact,
        Instant createdAt
) {

    public static VendorContactResponse from(VendorContact contact) {
        return new VendorContactResponse(
                contact.getId(),
                contact.getVendor().getId(),
                contact.getName(),
                contact.getEmail(),
                contact.getPhone(),
                contact.getDesignation(),
                contact.isPrimaryContact(),
                contact.getCreatedAt());
    }
}
