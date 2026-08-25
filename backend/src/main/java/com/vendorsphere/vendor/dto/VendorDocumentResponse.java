package com.vendorsphere.vendor.dto;

import com.vendorsphere.vendor.DocumentExpiryState;
import com.vendorsphere.vendor.entity.VendorDocument;
import com.vendorsphere.vendor.VendorDocumentType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record VendorDocumentResponse(
        UUID id,
        UUID vendorId,
        VendorDocumentType documentType,
        String fileName,
        String fileUrl,
        LocalDate expiryDate,
        DocumentExpiryState expiryState,
        Instant uploadedAt
) {

    public static VendorDocumentResponse from(VendorDocument document, DocumentExpiryState expiryState) {
        return new VendorDocumentResponse(
                document.getId(),
                document.getVendor().getId(),
                document.getDocumentType(),
                document.getFileName(),
                document.getFileUrl(),
                document.getExpiryDate(),
                expiryState,
                document.getUploadedAt());
    }
}
