package com.vendorsphere.vendor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vendorsphere.auth.security.UserPrincipal;
import com.vendorsphere.common.attachment.AttachmentOwnerType;
import com.vendorsphere.common.attachment.AttachmentResponse;
import com.vendorsphere.common.attachment.AttachmentService;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.service.NotificationService;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.user.RoleName;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.vendor.DocumentExpiryState;
import com.vendorsphere.vendor.VendorDocumentType;
import com.vendorsphere.vendor.dto.VendorDocumentRequest;
import com.vendorsphere.vendor.dto.VendorDocumentResponse;
import com.vendorsphere.vendor.entity.Vendor;
import com.vendorsphere.vendor.entity.VendorDocument;
import com.vendorsphere.vendor.repository.VendorDocumentRepository;
import com.vendorsphere.vendor.repository.VendorRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class VendorDocumentServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-14T09:15:30Z");
    private static final LocalDate TODAY = LocalDate.parse("2026-03-14");

    private final VendorRepository vendorRepository = mock(VendorRepository.class);
    private final VendorDocumentRepository vendorDocumentRepository =
            mock(VendorDocumentRepository.class);
    private final AttachmentService attachmentService = mock(AttachmentService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final VendorAccessGuard vendorAccessGuard = mock(VendorAccessGuard.class);

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final VendorDocumentService service = new VendorDocumentService(
            vendorDocumentRepository, vendorRepository, attachmentService,
            notificationService, vendorAccessGuard, clock);

    private final UUID organizationId = UUID.randomUUID();
    private Organization organization;

    @BeforeEach
    void authenticateCaller() {
        organization = new Organization();
        organization.setId(organizationId);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setOrganization(organization);
        user.setEmail("officer@demo-corp.com");
        user.setPasswordHash("hash");

        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(vendorDocumentRepository.save(any(VendorDocument.class)))
                .thenAnswer(call -> call.getArgument(0));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anUploadStoresTheMetadataAgainstTheVendor() {
        Vendor vendor = storedVendor();
        AttachmentResponse attachment = new AttachmentResponse(
                UUID.randomUUID(), AttachmentOwnerType.VENDOR_DOCUMENT, vendor.getId(),
                "gst.pdf", "application/pdf", 120L, UUID.randomUUID(), NOW);
        when(attachmentService.upload(
                eq(AttachmentOwnerType.VENDOR_DOCUMENT), eq(vendor.getId()), any()))
                .thenReturn(attachment);

        VendorDocumentResponse result = service.upload(vendor.getId(),
                new VendorDocumentRequest("GST_CERTIFICATE", LocalDate.parse("2026-06-30")),
                pdfFile());

        assertThat(result.documentType()).isEqualTo(VendorDocumentType.GST_CERTIFICATE);
        assertThat(result.fileName()).isEqualTo("gst.pdf");
        assertThat(result.fileUrl()).isEqualTo("/api/v1/attachments/" + attachment.id());
        assertThat(result.expiryDate()).isEqualTo(LocalDate.parse("2026-06-30"));
        // More than 30 days after the request date, so VALID rather than EXPIRING_SOON.
        assertThat(result.expiryState()).isEqualTo(DocumentExpiryState.VALID);
        assertThat(result.uploadedAt()).isEqualTo(NOW);

        ArgumentCaptor<VendorDocument> stored = ArgumentCaptor.forClass(VendorDocument.class);
        verify(vendorDocumentRepository).save(stored.capture());
        assertThat(stored.getValue().getVendor()).isSameAs(vendor);
    }

    @Test
    void anUploadInsideTheWindowReadsAsExpiringSoon() {
        Vendor vendor = storedVendor();
        when(attachmentService.upload(
                eq(AttachmentOwnerType.VENDOR_DOCUMENT), eq(vendor.getId()), any()))
                .thenReturn(attachmentOf(vendor));

        VendorDocumentResponse result = service.upload(vendor.getId(),
                new VendorDocumentRequest("AGREEMENT", TODAY.plusDays(30)), pdfFile());

        assertThat(result.expiryState()).isEqualTo(DocumentExpiryState.EXPIRING_SOON);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "GST_CERTIFICATE", "REGISTRATION_CERTIFICATE", "TAX_DOCUMENT",
            "COMPLIANCE_CERTIFICATE", "BANK_DETAILS", "AGREEMENT"})
    void everyDeclaredTypeIsAccepted(String rawType) {
        Vendor vendor = storedVendor();
        when(attachmentService.upload(
                eq(AttachmentOwnerType.VENDOR_DOCUMENT), eq(vendor.getId()), any()))
                .thenReturn(attachmentOf(vendor));

        VendorDocumentResponse result = service.upload(
                vendor.getId(), new VendorDocumentRequest(rawType, null), pdfFile());

        assertThat(result.documentType())
                .isEqualTo(VendorDocumentType.valueOf(rawType));
    }

    @Test
    void aMixedCaseTypeIsAccepted() {
        Vendor vendor = storedVendor();
        when(attachmentService.upload(
                eq(AttachmentOwnerType.VENDOR_DOCUMENT), eq(vendor.getId()), any()))
                .thenReturn(attachmentOf(vendor));

        VendorDocumentResponse result = service.upload(
                vendor.getId(), new VendorDocumentRequest("tax_document", null), pdfFile());

        assertThat(result.documentType()).isEqualTo(VendorDocumentType.TAX_DOCUMENT);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"PASSPORT"})
    void anUnknownTypeIsRejectedListingTheAcceptedTypes(String rawType) {
        Vendor vendor = storedVendor();

        assertThatThrownBy(() -> service.upload(
                vendor.getId(), new VendorDocumentRequest(rawType, null), pdfFile()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Unsupported vendor document type. Accepted document types: "
                        + "GST_CERTIFICATE, REGISTRATION_CERTIFICATE, TAX_DOCUMENT, "
                        + "COMPLIANCE_CERTIFICATE, BANK_DETAILS, AGREEMENT")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(attachmentService);
        verify(vendorDocumentRepository, never()).save(any());
    }

    @Test
    void uploadingForAVendorOfAnotherOrganizationIsNotFound() {
        UUID foreignVendorId = UUID.randomUUID();
        when(vendorRepository.findByIdAndOrganizationId(foreignVendorId, organizationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload(foreignVendorId,
                new VendorDocumentRequest("AGREEMENT", null), pdfFile()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor not found")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verifyNoInteractions(attachmentService);
    }

    @Test
    void aVendorUserDeniedTheVendorFailsClosed() {
        Vendor vendor = storedVendor();
        doThrow(new BusinessException("Vendor not found", HttpStatus.NOT_FOUND))
                .when(vendorAccessGuard).assertVendorVisible(vendor.getId(), "Vendor not found");

        assertThatThrownBy(() -> service.list(vendor.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor not found")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verifyNoInteractions(vendorDocumentRepository);
    }

    @Test
    void theListDerivesAnExpiryStatePerDocument() {
        Vendor vendor = storedVendor();
        when(vendorDocumentRepository
                .findByVendorIdAndVendorOrganizationIdOrderByUploadedAtDesc(
                        vendor.getId(), organizationId))
                .thenReturn(List.of(
                        document(vendor, VendorDocumentType.GST_CERTIFICATE,
                                TODAY.minusDays(1)),
                        document(vendor, VendorDocumentType.AGREEMENT, TODAY),
                        document(vendor, VendorDocumentType.BANK_DETAILS, null)));

        List<VendorDocumentResponse> result = service.list(vendor.getId());

        assertThat(result).extracting(VendorDocumentResponse::expiryState)
                .containsExactly(
                        DocumentExpiryState.EXPIRED,
                        DocumentExpiryState.EXPIRING_SOON,
                        DocumentExpiryState.VALID);
    }

    @Test
    void listingDocumentsOfAVendorOfAnotherOrganizationIsNotFound() {
        UUID foreignVendorId = UUID.randomUUID();
        when(vendorRepository.findByIdAndOrganizationId(foreignVendorId, organizationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.list(foreignVendorId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor not found");
    }

    @Test
    void theDailyEvaluationQueriesExactlyThreeDatesAndNotifiesBothRoles() {
        VendorDocument expiring = document(storedVendor(), VendorDocumentType.GST_CERTIFICATE,
                LocalDate.parse("2026-04-13"));

        when(vendorDocumentRepository.findByExpiryDateIn(anySet()))
                .thenReturn(List.of(expiring));

        service.notifyDocumentsExpiringOn(TODAY);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<LocalDate>> dates = ArgumentCaptor.forClass(Set.class);
        verify(vendorDocumentRepository).findByExpiryDateIn(dates.capture());
        assertThat(dates.getValue()).containsExactlyInAnyOrder(
                LocalDate.parse("2026-04-13"),
                LocalDate.parse("2026-03-21"),
                LocalDate.parse("2026-03-15"));

        verify(notificationService).createForRole(
                eq(organizationId), eq(RoleName.ADMIN),
                eq(NotificationEvent.VENDOR_DOCUMENT_EXPIRING),
                eq("VendorDocument"), eq(expiring.getId()), any(), any());
        verify(notificationService).createForRole(
                eq(organizationId), eq(RoleName.PROCUREMENT_OFFICER),
                eq(NotificationEvent.VENDOR_DOCUMENT_EXPIRING),
                eq("VendorDocument"), eq(expiring.getId()), any(), any());
    }

    @Test
    void aQuietDayNotifiesNobody() {
        when(vendorDocumentRepository.findByExpiryDateIn(anySet())).thenReturn(List.of());

        service.notifyDocumentsExpiringOn(TODAY);

        verifyNoInteractions(notificationService);
    }

    // ----- fixtures -----

    private Vendor storedVendor() {
        Vendor vendor = new Vendor();
        vendor.setId(UUID.randomUUID());
        vendor.setOrganization(organization);
        vendor.setVendorCode("VEN-2026-001");
        vendor.setCompanyName("Acme Supplies");
        vendor.setEmail("sales@acme.test");
        vendor.setStatus(com.vendorsphere.vendor.VendorStatus.ACTIVE);
        vendor.setRating(BigDecimal.ZERO.setScale(2));
        vendor.setRegisteredAt(NOW);
        when(vendorRepository.findByIdAndOrganizationId(vendor.getId(), organizationId))
                .thenReturn(Optional.of(vendor));
        return vendor;
    }

    private VendorDocument document(Vendor vendor, VendorDocumentType type, LocalDate expiryDate) {
        VendorDocument document = new VendorDocument();
        document.setVendor(vendor);
        document.setDocumentType(type);
        document.setFileName(type.name().toLowerCase() + ".pdf");
        document.setFileUrl("/api/v1/attachments/" + UUID.randomUUID());
        document.setExpiryDate(expiryDate);
        document.setUploadedAt(NOW);
        return document;
    }

    private AttachmentResponse attachmentOf(Vendor vendor) {
        return new AttachmentResponse(UUID.randomUUID(), AttachmentOwnerType.VENDOR_DOCUMENT,
                vendor.getId(), "document.pdf", "application/pdf", 100L, UUID.randomUUID(), NOW);
    }

    private MockMultipartFile pdfFile() {
        return new MockMultipartFile("file", "gst.pdf", "application/pdf", new byte[] {1, 2, 3});
    }
}
