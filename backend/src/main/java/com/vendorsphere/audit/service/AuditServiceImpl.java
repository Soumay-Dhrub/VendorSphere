package com.vendorsphere.audit.service;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.dto.AuditLogResponse;
import com.vendorsphere.audit.dto.AuditSearchCriteria;
import com.vendorsphere.audit.entity.AuditLog;
import com.vendorsphere.audit.repository.AuditLogRepository;
import com.vendorsphere.auth.security.UserPrincipal;
import com.vendorsphere.common.dto.PageResponse;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.common.util.PageSupport;
import com.vendorsphere.organization.repository.OrganizationRepository;
import com.vendorsphere.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuditServiceImpl implements AuditService {

    private static final int IP_ADDRESS_MAX_LENGTH = 45;

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String USER_AGENT_HEADER = "User-Agent";

    private final AuditLogRepository auditLogRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final AuditPayloadSerializer payloadSerializer;

    public AuditServiceImpl(
            AuditLogRepository auditLogRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            AuditPayloadSerializer payloadSerializer
    ) {
        this.auditLogRepository = auditLogRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.payloadSerializer = payloadSerializer;
    }

    @Override
    @Transactional
    public void record(
            AuditAction action,
            String entityType,
            UUID entityId,
            Object previous,
            Object current) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setPreviousValue(payloadSerializer.toJson(previous));
        auditLog.setNewValue(payloadSerializer.toJson(current));

        // A scheduled job changes state without an authenticated principal, and organization_id and
        // actor_id are nullable for exactly that case, so the actor is resolved defensively.
        currentPrincipal().ifPresent(principal -> {
            auditLog.setOrganization(
                    organizationRepository.getReferenceById(principal.getOrganizationId()));
            auditLog.setActor(userRepository.getReferenceById(principal.getId()));
        });

        currentRequest().ifPresent(request -> {
            auditLog.setIpAddress(resolveIpAddress(request));
            auditLog.setUserAgent(request.getHeader(USER_AGENT_HEADER));
        });

        auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(AuditSearchCriteria criteria, Pageable pageable) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return PageSupport.map(
                auditLogRepository.search(organizationId, criteria, pageable),
                AuditLogResponse::from);
    }

    private Optional<UserPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    private Optional<HttpServletRequest> currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return Optional.of(servletAttributes.getRequest());
        }
        return Optional.empty();
    }

    private String resolveIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        String candidate = forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr()
                : forwardedFor.split(",")[0].trim();

        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        return candidate.length() > IP_ADDRESS_MAX_LENGTH
                ? candidate.substring(0, IP_ADDRESS_MAX_LENGTH)
                : candidate;
    }
}
