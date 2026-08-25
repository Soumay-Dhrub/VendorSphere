package com.vendorsphere.rfq.dto;

import com.vendorsphere.rfq.RfqStatus;

import java.util.UUID;

/** Optional filters of the RFQ listing; an absent component contributes no predicate. */
public record RfqSearchCriteria(RfqStatus status) {
}
