package com.vendorsphere.vendor.dto;

import com.vendorsphere.vendor.entity.VendorContact;

import java.time.Instant;
import java.util.UUID;

/**
 * One vendor contact as returned by a create, update or list read (Requirements 4.1, 4.3).
 *
 * <p>{@code vendorId} is read from the contact's parent, which is already loaded by the service
 * before the contact is touched, so projecting it triggers no additional lazy load.
 */
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
