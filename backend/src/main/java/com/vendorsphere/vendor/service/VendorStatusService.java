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

/**
 * The vendor status lifecycle (Requirement 3).
 *
 * <p>The permitted pairs live in {@link VendorStatusTransitions#MACHINE}, which also owns the 409
 * {@code Cannot transition from X to Y} wording, so this service states no transition table of its
 * own (Requirements 3.1, 3.2).
 *
 * <p>Reads are keyed on the caller's organization, so a vendor identifier of another tenant misses
 * and surfaces as 404 {@code Vendor not found} rather than 403, keeping identifiers unenumerable
 * (Requirements 2.6, 30.10). The role grants of Requirement 3.5 are declared on the controller.
 */
@Service
public class VendorStatusService {

    /** Pinned by Requirement 3.4. */
    static final String REASON_REQUIRED_MESSAGE = "Status change reason is required";

    /** The three targets Requirement 3.4 names; every other target accepts an absent reason. */
    static final Set<VendorStatus> REASON_REQUIRED_TARGETS = EnumSet.of(
            VendorStatus.SUSPENDED, VendorStatus.BLACKLISTED, VendorStatus.INACTIVE);

    private final VendorRepository vendorRepository;
    private final AuditService auditService;

    public VendorStatusService(VendorRepository vendorRepository, AuditService auditService) {
        this.vendorRepository = vendorRepository;
        this.auditService = auditService;
    }

    /**
     * Moves a vendor to the requested status, persists the reason and records a
     * {@code VENDOR_STATUS_CHANGED} trail entry whose previous and new snapshots carry the previous
     * status, the new status and the reason (Requirement 3.3).
     *
     * <p>The transition is asserted before the reason, so a pair outside the acceptance criterion 3.1
     * table is reported as the 409 of Requirement 3.2 whether or not a reason accompanied it: the
     * request is invalid regardless of what else it carried. A permitted move to SUSPENDED,
     * BLACKLISTED or INACTIVE without a reason is then the 400 of Requirement 3.4.
     *
     * <p>A blank reason counts as no reason, so whitespace cannot satisfy Requirement 3.4. The stored
     * value is trimmed, and reset to {@code null} when the target needs none and none was supplied,
     * so the persisted reason always describes the latest change rather than an earlier one.
     *
     * @return the vendor's new status and reason
     */
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
