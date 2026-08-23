package com.vendorsphere.audit.controller;

import com.vendorsphere.audit.dto.AuditLogResponse;
import com.vendorsphere.audit.dto.AuditSearchCriteria;
import com.vendorsphere.audit.service.AuditService;
import com.vendorsphere.common.dto.ApiResponse;
import com.vendorsphere.common.dto.PageResponse;
import com.vendorsphere.common.util.PageSupport;
import com.vendorsphere.common.util.SortWhitelist;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only API over the audit trail.
 *
 * <p>Only {@code GET} is declared. That is the whole of the append-only enforcement at the HTTP
 * boundary: because no {@code PUT}, {@code PATCH} or {@code DELETE} handler exists for this path,
 * Spring MVC answers those methods with 405 on its own (Requirements 29.8, 29.9). Adding a write
 * endpoint here would silently break that guarantee, so do not.
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit")
public class AuditLogController {

    /**
     * Requirement 29.3 pins the default order to creation instant descending, which is why
     * {@code createdAt} is the default field and the direction defaults to {@code DESC} here rather
     * than to {@link PageSupport}'s ascending default.
     */
    private static final SortWhitelist SORTABLE =
            SortWhitelist.of("createdAt", "action", "entityType");

    private static final String DEFAULT_DIRECTION = "DESC";

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    /** Requirement 29.7: any role other than ADMIN is denied with 403. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List audit log entries of the current organization")
    public ApiResponse<PageResponse<AuditLogResponse>> listAuditLogs(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction
    ) {
        Pageable pageable = PageSupport.pageable(
                page,
                size,
                sort,
                direction == null || direction.isBlank() ? DEFAULT_DIRECTION : direction,
                SORTABLE);

        AuditSearchCriteria criteria =
                new AuditSearchCriteria(actorId, entityType, entityId, from, to);

        return ApiResponse.ok(auditService.search(criteria, pageable));
    }
}
