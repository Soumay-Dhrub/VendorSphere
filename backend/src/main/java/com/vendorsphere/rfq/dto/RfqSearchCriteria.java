package com.vendorsphere.rfq.dto;

import com.vendorsphere.rfq.RfqStatus;

import java.util.UUID;

public record RfqSearchCriteria(RfqStatus status) {
}
