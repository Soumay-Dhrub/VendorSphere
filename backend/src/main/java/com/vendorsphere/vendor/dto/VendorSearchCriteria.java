package com.vendorsphere.vendor.dto;

import com.vendorsphere.vendor.VendorStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The four optional filters of a vendor listing (Requirements 6.2 through 6.5). Every component is
 * nullable and an absent component simply contributes no predicate, so the filters combine with AND
 * and any subset of them narrows the same result set (Requirement 6.6).
 *
 * <p>The organization is deliberately <em>not</em> a component. Tenant scope is supplied by the
 * service from {@code SecurityUtils.getCurrentOrganizationId()} and applied unconditionally, so no
 * request payload can widen or bypass it (Requirements 6.1, 30.10).
 */
public record VendorSearchCriteria(
        String companyName,
        UUID categoryId,
        VendorStatus status,
        BigDecimal minRating
) {

    /** An unfiltered listing, which returns every vendor of the caller's organization. */
    public static VendorSearchCriteria none() {
        return new VendorSearchCriteria(null, null, null, null);
    }

    /**
     * The company-name term with surrounding whitespace removed, or {@code null} when no usable term
     * was supplied. A blank term is treated as absent rather than as "contains the empty string",
     * which would otherwise turn an empty search box into a filter that happens to match everything.
     */
    public String companyNameTerm() {
        return companyName == null || companyName.isBlank() ? null : companyName.trim();
    }
}
