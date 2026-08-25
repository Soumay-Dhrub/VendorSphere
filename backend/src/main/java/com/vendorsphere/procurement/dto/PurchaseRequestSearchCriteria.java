package com.vendorsphere.procurement.dto;

import com.vendorsphere.procurement.PurchaseRequestStatus;

import java.util.UUID;

public record PurchaseRequestSearchCriteria(
        PurchaseRequestStatus status,
        UUID departmentId
) {

    public static PurchaseRequestSearchCriteria none() {
        return new PurchaseRequestSearchCriteria(null, null);
    }
}
