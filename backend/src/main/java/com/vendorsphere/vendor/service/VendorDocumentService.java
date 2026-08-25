package com.vendorsphere.vendor.service;

import com.vendorsphere.common.attachment.AttachmentOwnerType;
import com.vendorsphere.common.attachment.AttachmentResponse;
import com.vendorsphere.common.attachment.AttachmentService;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.service.NotificationService;
import com.vendorsphere.user.RoleName;
import com.vendorsphere.vendor.DocumentExpiryEvaluator;
import com.vendorsphere.vendor.DocumentExpiryState;
import com.vendorsphere.vendor.VendorDocumentType;
import com.vendorsphere.vendor.dto.VendorDocumentRequest;
import com.vendorsphere.vendor.dto.VendorDocumentResponse;
import com.vendorsphere.vendor.entity.Vendor;
import com.vendorsphere.vendor.entity.VendorDocument;
import com.vendorsphere.vendor.repository.VendorDocumentRepository;
import com.vendorsphere.vendor.repository.VendorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class VendorDocumentService {

    static final int EXACTLY_7_DAYS_AHEAD = 7;
    static final int EXACTLY_1_DAY_AHEAD = 1;

    static final String UNSUPPORTED_TYPE_MESSAGE =
            "Unsupported vendor document type. Accepted document types: "
                    + String.join(", ", typeNames());

    private final VendorDocumentRepository vendorDocumentRepository;
    private final VendorRepository vendorRepository;
    private final AttachmentService attachmentService;
    private final NotificationService notificationService;
    private final VendorAccessGuard vendorAccessGuard;
    private final Clock clock;

    public VendorDocumentService(
            VendorDocumentRepository vendorDocumentRepository,
            VendorRepository vendorRepository,
            AttachmentService attachmentService,
            NotificationService notificationService,
            VendorAccessGuard vendorAccessGuard,
            Clock clock
    ) {
        this.vendorDocumentRepository = vendorDocumentRepository;
        this.vendorRepository = vendorRepository;
        this.attachmentService = attachmentService;
        this.notificationService = notificationService;
        this.vendorAccessGuard = vendorAccessGuard;
        this.clock = clock;
    }

    @Transactional
    public VendorDocumentResponse upload(
            UUID vendorId, VendorDocumentRequest request, MultipartFile file) {
        VendorDocumentType documentType = parseType(request.documentType());
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Vendor vendor = findVisibleVendor(vendorId, organizationId);

        AttachmentResponse attachment = attachmentService.upload(
                AttachmentOwnerType.VENDOR_DOCUMENT, vendor.getId(), file);

        VendorDocument document = new VendorDocument();
        document.setVendor(vendor);
        document.setDocumentType(documentType);
        document.setFileName(attachment.originalFilename());
        document.setFileUrl("/api/v1/attachments/" + attachment.id());
        document.setExpiryDate(request.expiryDate());
        document.setUploadedAt(clock.instant());

        return VendorDocumentResponse.from(
                vendorDocumentRepository.save(document), expiryStateOf(document));
    }

    @Transactional(readOnly = true)
    public List<VendorDocumentResponse> list(UUID vendorId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        findVisibleVendor(vendorId, organizationId);
        LocalDate today = LocalDate.now(clock);
        return vendorDocumentRepository
                .findByVendorIdAndVendorOrganizationIdOrderByUploadedAtDesc(vendorId, organizationId)
                .stream()
                .map(document -> VendorDocumentResponse.from(
                        document, DocumentExpiryEvaluator.evaluate(document.getExpiryDate(), today)))
                .toList();
    }

    private Vendor findVisibleVendor(UUID vendorId, UUID organizationId) {
        Vendor vendor = vendorRepository.findByIdAndOrganizationId(vendorId, organizationId)
                .orElseThrow(() -> new BusinessException(
                        VendorService.NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        vendorAccessGuard.assertVendorVisible(vendor.getId(), VendorService.NOT_FOUND_MESSAGE);
        return vendor;
    }

    @Transactional
    public void notifyDocumentsExpiringOn(LocalDate evaluationDate) {
        Set<LocalDate> targetDates = Set.of(
                evaluationDate.plusDays(DocumentExpiryEvaluator.EXPIRING_SOON_WINDOW_DAYS),
                evaluationDate.plusDays(EXACTLY_7_DAYS_AHEAD),
                evaluationDate.plusDays(EXACTLY_1_DAY_AHEAD));

        for (VendorDocument document : vendorDocumentRepository.findByExpiryDateIn(targetDates)) {
            UUID organizationId = document.getVendor().getOrganization().getId();
            String message = document.getDocumentType() + " of vendor "
                    + document.getVendor().getCompanyName()
                    + " expires on " + document.getExpiryDate() + ".";
            notifyRoles(organizationId, document.getId(), message);
        }
    }

    private void notifyRoles(UUID organizationId, UUID documentId, String message) {
        String title = "Vendor document expiring";
        notificationService.createForRole(organizationId, RoleName.ADMIN,
                NotificationEvent.VENDOR_DOCUMENT_EXPIRING,
                "VendorDocument", documentId, title, message);
        notificationService.createForRole(organizationId, RoleName.PROCUREMENT_OFFICER,
                NotificationEvent.VENDOR_DOCUMENT_EXPIRING,
                "VendorDocument", documentId, title, message);
    }

    private DocumentExpiryState expiryStateOf(VendorDocument document) {
        return DocumentExpiryEvaluator.evaluate(document.getExpiryDate(), LocalDate.now(clock));
    }

    private static VendorDocumentType parseType(String raw) {
        if (raw != null) {
            try {
                return VendorDocumentType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // An unknown or absent type falls through to the pinned rejection below.
            }
        }
        throw new BusinessException(UNSUPPORTED_TYPE_MESSAGE, HttpStatus.BAD_REQUEST);
    }

    private static List<String> typeNames() {
        return Arrays.stream(VendorDocumentType.values()).map(Enum::name).toList();
    }
}
