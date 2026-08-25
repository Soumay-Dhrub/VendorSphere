package com.vendorsphere.audit.repository;

import com.vendorsphere.audit.entity.AuditLog;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface AuditLogRepository extends Repository<AuditLog, UUID>, AuditLogSearch {

    AuditLog save(AuditLog auditLog);
}
