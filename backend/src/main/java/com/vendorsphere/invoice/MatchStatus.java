package com.vendorsphere.invoice;

public enum MatchStatus {

    PENDING,

    MATCHED,

    QUANTITY_MISMATCH,

    PRICE_MISMATCH,

    MISSING_DELIVERY,

    DUPLICATE_INVOICE
}
