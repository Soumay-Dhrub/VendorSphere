package com.vendorsphere.rfq.service;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.service.AuditService;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.service.NotificationService;
import com.vendorsphere.rfq.RfqStatus;
import com.vendorsphere.rfq.RfqVendorStatus;
import com.vendorsphere.rfq.dto.RfqInviteRequest;
import com.vendorsphere.rfq.dto.RfqResponse;
import com.vendorsphere.rfq.entity.Rfq;
import com.vendorsphere.rfq.entity.RfqVendor;
import com.vendorsphere.rfq.repository.RfqRepository;
import com.vendorsphere.rfq.repository.RfqVendorRepository;
import com.vendorsphere.user.RoleName;
import com.vendorsphere.user.repository.UserRepository;
import com.vendorsphere.vendor.VendorStatus;
import com.vendorsphere.vendor.entity.Vendor;
import com.vendorsphere.vendor.repository.VendorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RfqVendorService {

    static final String ALREADY_INVITED_MESSAGE = "Vendor already invited to this RFQ";

    static final String NOT_FOUND_MESSAGE = "RFQ not found";
    static final String VENDOR_NOT_FOUND_MESSAGE = "Vendor not found";

    public static final Set<RfqStatus> VENDOR_VISIBLE_STATUSES =
            EnumSet.of(RfqStatus.OPEN, RfqStatus.CLOSED, RfqStatus.EVALUATION, RfqStatus.AWARDED);

    private final RfqVendorRepository rfqVendorRepository;
    private final RfqRepository rfqRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final Clock clock;

    public RfqVendorService(
            RfqVendorRepository rfqVendorRepository,
            RfqRepository rfqRepository,
            VendorRepository vendorRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            AuditService auditService,
            Clock clock
    ) {
        this.rfqVendorRepository = rfqVendorRepository;
        this.rfqRepository = rfqRepository;
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public List<RfqResponse.RfqVendorResponse> invite(UUID rfqId, RfqInviteRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Rfq rfq = rfqRepository.findByIdAndOrganizationId(rfqId, organizationId)
                .orElseThrow(() -> new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));

        List<Vendor> vendors = new ArrayList<>();
        for (UUID vendorId : request.vendorIds()) {
            Vendor vendor = vendorRepository.findByIdAndOrganizationId(vendorId, organizationId)
                    .orElseThrow(() -> new BusinessException(
                            VENDOR_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
            if (vendor.getStatus() != VendorStatus.ACTIVE) {
                throw new BusinessException(inactiveMessage(vendor), HttpStatus.CONFLICT);
            }
            if (rfqVendorRepository.findByRfqIdAndVendorId(rfq.getId(), vendor.getId()).isPresent()) {
                throw new BusinessException(ALREADY_INVITED_MESSAGE, HttpStatus.CONFLICT);
            }
            vendors.add(vendor);
        }

        var inviter = userRepository.getReferenceById(SecurityUtils.getCurrentUserId());
        List<RfqVendor> created = new ArrayList<>();
        for (Vendor vendor : vendors) {
            RfqVendor invitation = new RfqVendor();
            invitation.setRfq(rfq);
            invitation.setVendor(vendor);
            invitation.setInvitedAt(clock.instant());
            invitation.setInvitedBy(inviter);
            created.add(rfqVendorRepository.save(invitation));
        }

        auditService.record(AuditAction.VENDOR_INVITED, "Rfq", rfq.getId(), null,
                vendors.stream().map(Vendor::getId).toList());

        if (rfq.getStatus() == RfqStatus.OPEN) {
            for (Vendor vendor : vendors) {
                notifyVendorUsers(vendor.getId(), rfq);
            }
        }
        return created.stream().map(RfqResponse.RfqVendorResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<Rfq> listForVendor(UUID organizationId, UUID vendorId) {
        return rfqRepository.findInvitedForVendor(
                organizationId, vendorId, VENDOR_VISIBLE_STATUSES,
                org.springframework.data.domain.PageRequest.of(0, 100));
    }

    @Transactional
    public Rfq getForVendor(UUID organizationId, UUID vendorId, UUID rfqId) {
        RfqVendor invitation = rfqVendorRepository
                .findByRfqIdAndVendorIdAndVendorOrganizationId(rfqId, vendorId, organizationId)
                .orElseThrow(() -> new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        Rfq rfq = invitation.getRfq();
        if (!VENDOR_VISIBLE_STATUSES.contains(rfq.getStatus())) {
            throw new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND);
        }
        if (invitation.getStatus() == RfqVendorStatus.INVITED) {
            invitation.setStatus(RfqVendorStatus.VIEWED);
            rfqVendorRepository.save(invitation);
        }
        return rfq;
    }

    static String inactiveMessage(Vendor vendor) {
        return "Vendor " + vendor.getCompanyName() + " is " + vendor.getStatus();
    }

    private void notifyVendorUsers(UUID vendorId, Rfq rfq) {
        notificationService.createForVendorUsers(vendorId,
                NotificationEvent.VENDOR_INVITED_TO_RFQ, "Rfq", rfq.getId(),
                "Invited to quote",
                rfq.getRfqNumber() + " - " + rfq.getTitle()
                        + " closes at " + rfq.getClosingDate() + ".");
    }
}
