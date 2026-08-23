package com.vendorsphere.vendor.entity;

import com.vendorsphere.common.entity.CreatedOnlyEntity;
import com.vendorsphere.vendor.VendorDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Compliance document metadata stored against a vendor (Requirement 5.1).
 *
 * <p>{@code vendor_documents} carries {@code created_at} but no {@code updated_at}, so this entity
 * extends {@link CreatedOnlyEntity}.
 *
 * <p>{@code document_type} is a plain {@code VARCHAR(100)} in V1 with no {@code CHECK} constraint;
 * the accepted-type allowlist of Requirement 5.2 is enforced in the service layer. The column is
 * mapped as {@link VendorDocumentType} regardless, so the type is a closed set in Java.
 *
 * <p>The expiry state of Requirement 5.4 is derived on read from {@code expiryDate} and is not
 * stored.
 */
@Entity
@Table(name = "vendor_documents")
public class VendorDocument extends CreatedOnlyEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 100)
    private VendorDocumentType documentType;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public VendorDocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(VendorDocumentType documentType) {
        this.documentType = documentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
