package com.vendorsphere.procurement.dto;

import com.vendorsphere.procurement.PurchaseRequestStatus;

import java.util.UUID;

/**
 * The optional filters of a purchase request listing. Every component is nullable and an absent
 * component contributes no predicate, so any subset of them narrows the same result set.
 *
 * <p>Neither the organization nor the requester is a component: tenant scope and the requester-only
 * narrowing arrive from the service out of the security context, so no request parameter can widen or
 * bypass them (Requirements 30.10, 8.9).
 */
public record PurchaseRequestSearchCriteria(
        PurchaseRequestStatus status,
        UUID departmentId
) {

    /** An unfiltered listing, which returns every visible request of the caller's organization. */
    public static PurchaseRequestSearchCriteria none() {
        return new PurchaseRequestSearchCriteria(null, null);
    }
}
