package com.vendorsphere.vendor.service;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.service.AuditService;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.vendor.VendorStatus;
import com.vendorsphere.vendor.VendorStatusTransitions;
import com.vendorsphere.vendor.dto.VendorStatusChangeRequest;
import com.vendorsphere.vendor.dto.VendorStatusSnapshot;
import com.vendorsphere.vendor.entity.Vendor;
import com.vendorsphere.vendor.repository.VendorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
public class VendorStatusService {

    static final String REASON_REQUIRED_MESSAGE = "Status change reason is required";

    static final Set<VendorStatus> REASON_REQUIRED_TARGETS = EnumSet.of(
            VendorStatus.SUSPENDED, VendorStatus.BLACKLISTED, VendorStatus.INACTIVE);

    private final VendorRepository vendorRepository;
    private final AuditService auditService;

    public VendorStatusService(VendorRepository vendorRepository, AuditService auditService) {
        this.vendorRepository = vendorRepository;
        this.auditService = auditService;
    }

    @Transactional
    public VendorStatusSnapshot changeStatus(UUID vendorId, VendorStatusChangeRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Vendor vendor = vendorRepository.findByIdAndOrganizationId(vendorId, organizationId)
                .orElseThrow(() -> new BusinessException(
                        VendorService.NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));

        VendorStatus target = request.status();
        VendorStatusTransitions.MACHINE.assertTransition(vendor.getStatus(), target);

        String reason = normalizeReason(request.reason());
        if (reason == null && REASON_REQUIRED_TARGETS.contains(target)) {
            throw new BusinessException(REASON_REQUIRED_MESSAGE, HttpStatus.BAD_REQUEST);
        }

        VendorStatusSnapshot previous = VendorStatusSnapshot.from(vendor);
        vendor.setStatus(target);
        vendor.setStatusChangeReason(reason);

        Vendor saved = vendorRepository.save(vendor);
        VendorStatusSnapshot current = VendorStatusSnapshot.from(saved);
        auditService.record(AuditAction.VENDOR_STATUS_CHANGED, "Vendor", saved.getId(),
                previous, current);
        return current;
    }

    private static String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        return reason.trim();
    }
}
