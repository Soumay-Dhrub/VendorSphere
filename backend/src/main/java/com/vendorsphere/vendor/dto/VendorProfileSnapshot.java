package com.vendorsphere.vendor.dto;

import com.vendorsphere.vendor.VendorStatus;
import com.vendorsphere.vendor.entity.Vendor;

import java.math.BigDecimal;
import java.util.UUID;

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
