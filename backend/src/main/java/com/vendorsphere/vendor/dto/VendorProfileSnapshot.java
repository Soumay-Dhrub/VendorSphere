package com.vendorsphere.vendor.dto;

import com.vendorsphere.vendor.VendorStatus;
import com.vendorsphere.vendor.entity.Vendor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The audited state of a vendor profile, used as the previous and new value of the
 * {@code VENDOR_CREATED} and {@code VENDOR_UPDATED} trail entries (Requirement 29.1).
 *
 * <p>A purpose-built record rather than the {@link Vendor} entity, because serializing an entity
 * would drag its lazy associations into the stored document. It also stays free of the derived
 * figures {@link VendorResponse} carries, so an audit row records only state the change actually
 * touched.
 */
public record VendorProfileSnapshot(
        UUID id,
        String vendorCode,
        String companyName,
        String contactPerson,
        String email,
        String phone,
        String address,
        String taxIdentifier,
        VendorStatus status,
        BigDecimal rating,
        UUID categoryId
) {

    public static VendorProfileSnapshot from(Vendor vendor) {
        return new VendorProfileSnapshot(
                vendor.getId(),
                vendor.getVendorCode(),
                vendor.getCompanyName(),
                vendor.getContactPerson(),
                vendor.getEmail(),
                vendor.getPhone(),
                vendor.getAddress(),
                vendor.getTaxIdentifier(),
                vendor.getStatus(),
                vendor.getRating(),
                vendor.getCategory() != null ? vendor.getCategory().getId() : null);
    }
}
