package com.vendorsphere.audit.service;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.dto.AuditLogResponse;
import com.vendorsphere.audit.dto.AuditSearchCriteria;
import com.vendorsphere.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AuditService {

    void record(AuditAction action, String entityType, UUID entityId, Object previous, Object current);

    PageResponse<AuditLogResponse> search(AuditSearchCriteria criteria, Pageable pageable);
}
