package com.vendorsphere.vendor.dto;

import java.time.LocalDate;

public record VendorDocumentRequest(
        String documentType,
        LocalDate expiryDate
) {
}
