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

/**
 * The compliance documents stored against a vendor (Requirement 5).
 *
 * <p>The file itself is stored by {@link AttachmentService} under the {@code VENDOR_DOCUMENT} owner
 * type, so content-type and size rules live in one place for the whole platform; this service stores
 * the metadata row a vendor profile reads. The accepted-type allowlist of Requirement 5.2 is enforced
 * here rather than at the message converter, so an unknown type is answered with the pinned 400 that
 * lists the six types instead of a generic deserialization error (Requirement 5.3).
 *
 * <p>Reads are keyed on the caller's organization, so a vendor identifier belonging to another tenant
 * misses and surfaces as 404 {@code Vendor not found} - the wording {@link VendorService} pins -
 * rather than 403 (Requirements 2.6, 30.10). Whether the caller may see the vendor at all when they
 * hold the VENDOR role is decided by {@link VendorAccessGuard}: a vendor user reaches only its own
 * documents (Requirement 30.8).
 *
 * <p>Expiry states are derived on read from {@link DocumentExpiryEvaluator}, never stored, so a list
 * response always reflects the request date.
 */
@Service
public class VendorDocumentService {

    /** Days ahead of the evaluation date the job nudges besides the window boundary, pinned by 5.5. */
    static final int EXACTLY_7_DAYS_AHEAD = 7;
    static final int EXACTLY_1_DAY_AHEAD = 1;

    /**
     * The 400 wording of Requirement 5.3, which asks that the rejected upload be told what would have
     * been accepted. The order follows {@link VendorDocumentType} declaration order, matching how
     * Requirement 5.2 lists them.
     */
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

    /**
     * Stores one compliance document against the vendor (Requirement 5.1).
     *
     * <p>The type allowlist is asserted before any byte is written and before the vendor lookup, so
     * an unknown type costs no query; the file gates then run inside the attachment service, whose
     * owner-access hook consults the same guard the read paths use. The stored metadata row records
     * the upload timestamp from the clock, and {@code fileUrl} points at the platform's download path
     * for the created attachment.
     */
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

    /**
     * Lists the vendor's documents, newest upload first, each carrying its expiry state derived
     * against the request date (Requirement 5.4).
     */
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

    /**
     * Loads the vendor within the caller's organization and applies the vendor-user restriction of
     * Requirement 30.8: a caller holding the VENDOR role reaches only its own documents. The guard
     * reports a denial as 404 with the pinned vendor wording, so another vendor's document set is
     * indistinguishable from an unknown vendor.
     */
    private Vendor findVisibleVendor(UUID vendorId, UUID organizationId) {
        Vendor vendor = vendorRepository.findByIdAndOrganizationId(vendorId, organizationId)
                .orElseThrow(() -> new BusinessException(
                        VendorService.NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        vendorAccessGuard.assertVendorVisible(vendor.getId(), VendorService.NOT_FOUND_MESSAGE);
        return vendor;
    }

    /**
     * The daily expiry evaluation behind the scheduled job (Requirement 5.5): one notification per
     * ADMIN and per PROCUREMENT_OFFICER user of the owning organization for every document expiring
     * exactly 30, 7 or 1 day after {@code evaluationDate}.
     *
     * <p>The 30-day target comes from {@link DocumentExpiryEvaluator#EXPIRING_SOON_WINDOW_DAYS}
     * rather than being restated, so the job can never drift away from the window it mirrors. Each
     * document fans out through {@link NotificationService#createForRole}, whose write-time dedupe
     * keeps a re-run of the job on the same day from notifying anyone twice.
     */
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
