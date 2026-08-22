package com.vendorsphere.vendor;

/**
 * Accepted vendor compliance document types (Requirement 5.2).
 *
 * <p>Persisted as {@code VARCHAR} through {@code @Enumerated(EnumType.STRING)}.
 */
public enum VendorDocumentType {

    /** Goods and services tax registration certificate. */
    GST_CERTIFICATE,

    /** Company or business registration certificate. */
    REGISTRATION_CERTIFICATE,

    /** Tax-related document such as a tax residency or withholding certificate. */
    TAX_DOCUMENT,

    /** Regulatory or quality compliance certificate. */
    COMPLIANCE_CERTIFICATE,

    /** Bank account details for payment settlement. */
    BANK_DETAILS,

    /** Signed supply or framework agreement. */
    AGREEMENT
}
