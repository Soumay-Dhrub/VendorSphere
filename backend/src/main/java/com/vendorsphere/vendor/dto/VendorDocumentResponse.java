package com.vendorsphere.vendor.dto;

import com.vendorsphere.vendor.DocumentExpiryState;
import com.vendorsphere.vendor.entity.VendorDocument;
import com.vendorsphere.vendor.VendorDocumentType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One stored vendor compliance document together with its derived expiry state (Requirement 5.4).
 *
 * <p>The state is computed by the service from {@code expiryDate} and the request date rather than
 * being read off the entity, because it is derived data that must move with the calendar, not a
 * stored fact that can go stale. {@code fileUrl} points at the platform's attachment download path,
 * so a client can fetch the bytes without knowing how files are stored.
 */
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

    /**
     * Projects a stored document; the caller supplies the already-derived expiry state so this record
     * owns no clock and no classification rule of its own.
     */
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
