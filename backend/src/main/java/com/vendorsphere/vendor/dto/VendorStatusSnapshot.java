package com.vendorsphere.vendor.dto;

import com.vendorsphere.vendor.VendorStatus;
import com.vendorsphere.vendor.entity.Vendor;

import java.util.UUID;

public record VendorStatusSnapshot(
        UUID vendorId,
        VendorStatus status,
        String reason
) {

    public static VendorStatusSnapshot from(Vendor vendor) {
        return new VendorStatusSnapshot(
                vendor.getId(), vendor.getStatus(), vendor.getStatusChangeReason());
    }
}
