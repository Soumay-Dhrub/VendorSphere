package com.vendorsphere.audit.repository;

import com.vendorsphere.audit.dto.AuditSearchCriteria;
import com.vendorsphere.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AuditLogSearch {

    Page<AuditLog> search(UUID organizationId, AuditSearchCriteria criteria, Pageable pageable);
}
