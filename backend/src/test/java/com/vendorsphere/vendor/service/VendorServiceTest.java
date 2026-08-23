package com.vendorsphere.vendor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.service.AuditService;
import com.vendorsphere.auth.security.UserPrincipal;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.util.ReferenceNumberGenerator;
import com.vendorsphere.common.util.ReferencePrefix;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.organization.repository.OrganizationRepository;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.vendor.VendorStatus;
import com.vendorsphere.vendor.dto.VendorRequest;
import com.vendorsphere.vendor.dto.VendorResponse;
import com.vendorsphere.vendor.entity.Vendor;
import com.vendorsphere.vendor.entity.VendorCategory;
import com.vendorsphere.vendor.repository.VendorCategoryRepository;
import com.vendorsphere.vendor.repository.VendorDocumentRepository;
import com.vendorsphere.vendor.repository.VendorRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The creation defaults of Requirement 2.1, the update rule of Requirement 2.4 and the two messages
 * Requirements 2.3 and 2.6 pin. Mapping and tenant scoping of the finders themselves are exercised
 * against PostgreSQL in {@code VendorRepositoryTest}.
 */
class VendorServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-14T09:15:30Z");

    private final VendorRepository vendorRepository = mock(VendorRepository.class);
    private final VendorCategoryRepository vendorCategoryRepository =
            mock(VendorCategoryRepository.class);
    private final VendorDocumentRepository vendorDocumentRepository =
            mock(VendorDocumentRepository.class);
    private final OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
    private final ReferenceNumberGenerator referenceNumberGenerator =
            mock(ReferenceNumberGenerator.class);
    private final AuditService auditService = mock(AuditService.class);

    private final VendorService service = new VendorService(
            vendorRepository,
            vendorCategoryRepository,
            vendorDocumentRepository,
            organizationRepository,
            referenceNumberGenerator,
            auditService,
            Clock.fixed(NOW, ZoneOffset.UTC));

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

        when(organizationRepository.getReferenceById(organizationId)).thenReturn(organization);
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> {
            Vendor vendor = invocation.getArgument(0);
            if (vendor.getId() == null) {
                vendor.setId(UUID.randomUUID());
            }
            return vendor;
        });
        when(vendorRepository.findLatestPerformanceScore(any())).thenReturn(Optional.empty());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /** Requirement 2.1. */
    @Test
    void registrationAppliesTheGeneratedCodeProspectiveStatusZeroRatingAndRegistrationInstant() {
        when(referenceNumberGenerator.allocate(organizationId, ReferencePrefix.VEN))
                .thenReturn("VEN-2026-001");

        VendorResponse response = service.register(request("Acme Supplies", "sales@acme.test", null));

        assertThat(response.vendorCode()).isEqualTo("VEN-2026-001");
        assertThat(response.status()).isEqualTo(VendorStatus.PROSPECTIVE);
        assertThat(response.rating()).isEqualByComparingTo("0.00");
        assertThat(response.rating().scale()).isEqualTo(2);
        assertThat(response.registeredAt()).isEqualTo(NOW);
        assertThat(response.companyName()).isEqualTo("Acme Supplies");
        assertThat(response.performanceScore()).isEqualByComparingTo("0.00");
        verify(auditService).record(
                eq(AuditAction.VENDOR_CREATED), eq("Vendor"), eq(response.id()), eq(null), any());
    }

    /** Requirement 2.3: uniqueness is scoped to the caller's organization. */
    @Test
    void registeringADuplicateEmailInTheSameOrganizationIsRejected() {
        when(vendorRepository.existsByOrganizationIdAndEmailIgnoreCase(organizationId, "sales@acme.test"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.register(request("Acme Supplies", "sales@acme.test", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor email already registered")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(vendorRepository, never()).save(any());
        verify(referenceNumberGenerator, never()).allocate(any(), any());
    }

    /** Requirement 2.6: a cross-tenant or unknown identifier is not found, never forbidden. */
    @Test
    void readingAVendorOfAnotherOrganizationIsNotFound() {
        UUID foreignVendorId = UUID.randomUUID();
        when(vendorRepository.findByIdAndOrganizationId(foreignVendorId, organizationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(foreignVendorId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor not found")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    /** Requirement 2.6 applies to writes as well as reads. */
    @Test
    void updatingAVendorOfAnotherOrganizationIsNotFound() {
        UUID foreignVendorId = UUID.randomUUID();
        when(vendorRepository.findByIdAndOrganizationId(foreignVendorId, organizationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.update(foreignVendorId, request("Renamed", "new@acme.test", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor not found")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(vendorRepository, never()).save(any());
    }

    /** Requirement 2.4: the profile fields change, status and rating do not. */
    @Test
    void updateAppliesProfileFieldsAndLeavesStatusAndRatingUnchanged() {
        Vendor existing = existingVendor();
        existing.setStatus(VendorStatus.ACTIVE);
        existing.setRating(new BigDecimal("4.25"));
        when(vendorRepository.findByIdAndOrganizationId(existing.getId(), organizationId))
                .thenReturn(Optional.of(existing));

        VendorCategory category = new VendorCategory();
        category.setId(UUID.randomUUID());
        category.setName("Hardware");
        when(vendorCategoryRepository.findByIdAndOrganizationId(category.getId(), organizationId))
                .thenReturn(Optional.of(category));

        VendorResponse response = service.update(existing.getId(),
                new VendorRequest("Acme Industrial", "Riya Nair", "sales@acme.test",
                        "+91-9000000000", "12 Industrial Estate", "29ABCDE1234F1Z5", category.getId()));

        assertThat(response.companyName()).isEqualTo("Acme Industrial");
        assertThat(response.contactPerson()).isEqualTo("Riya Nair");
        assertThat(response.taxIdentifier()).isEqualTo("29ABCDE1234F1Z5");
        assertThat(response.categoryName()).isEqualTo("Hardware");
        assertThat(response.status()).isEqualTo(VendorStatus.ACTIVE);
        assertThat(response.rating()).isEqualByComparingTo("4.25");
        verify(auditService).record(
                eq(AuditAction.VENDOR_UPDATED), eq("Vendor"), eq(existing.getId()), any(), any());
    }

    /** Requirement 2.3: moving to an address another vendor already holds is still a clash. */
    @Test
    void updatingToAnEmailHeldByAnotherVendorIsRejectedButKeepingTheOwnEmailIsNot() {
        Vendor existing = existingVendor();
        when(vendorRepository.findByIdAndOrganizationId(existing.getId(), organizationId))
                .thenReturn(Optional.of(existing));
        when(vendorRepository.existsByOrganizationIdAndEmailIgnoreCase(organizationId, "taken@beta.test"))
                .thenReturn(true);

        assertThatThrownBy(() ->
                service.update(existing.getId(), request("Acme Supplies", "taken@beta.test", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor email already registered")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        // The vendor's own address, in different case, is not a duplicate of itself.
        service.update(existing.getId(), request("Acme Supplies", "SALES@ACME.TEST", null));

        verify(vendorRepository).save(existing);
    }

    /**
     * Requirement 2.5: a detail read carries the category name, the performance score and the count
     * of documents expiring within 30 days of the request date.
     */
    @Test
    void readReturnsCategoryNamePerformanceScoreAndExpiringDocumentCount() {
        Vendor existing = existingVendor();
        existing.setRating(new BigDecimal("4.25"));
        VendorCategory category = new VendorCategory();
        category.setId(UUID.randomUUID());
        category.setName("Logistics");
        existing.setCategory(category);
        when(vendorRepository.findByIdAndOrganizationId(existing.getId(), organizationId))
                .thenReturn(Optional.of(existing));
        when(vendorDocumentRepository.countByVendorIdAndVendorOrganizationIdAndExpiryDateBetween(
                existing.getId(), organizationId,
                LocalDate.of(2026, 3, 14), LocalDate.of(2026, 4, 13)))
                .thenReturn(2L);

        VendorResponse response = service.get(existing.getId());

        assertThat(response.categoryId()).isEqualTo(category.getId());
        assertThat(response.categoryName()).isEqualTo("Logistics");
        assertThat(response.expiringDocumentCount()).isEqualTo(2L);
        // No snapshot yet, so the score is the inverse of the engine's score / 20 rating mapping.
        assertThat(response.performanceScore()).isEqualByComparingTo("85.00");
    }

    /** A recorded snapshot is authoritative over the rating-derived fallback. */
    @Test
    void readPrefersTheLatestPerformanceSnapshotScore() {
        Vendor existing = existingVendor();
        existing.setRating(new BigDecimal("4.25"));
        when(vendorRepository.findByIdAndOrganizationId(existing.getId(), organizationId))
                .thenReturn(Optional.of(existing));
        when(vendorRepository.findLatestPerformanceScore(existing.getId()))
                .thenReturn(Optional.of(new BigDecimal("72.4")));

        assertThat(service.get(existing.getId()).performanceScore()).isEqualByComparingTo("72.40");
    }

    // ----- fixtures -----

    private VendorRequest request(String companyName, String email, UUID categoryId) {
        return new VendorRequest(companyName, "Riya Nair", email, "+91-9000000000",
                "12 Industrial Estate", "29ABCDE1234F1Z5", categoryId);
    }

    private Vendor existingVendor() {
        Vendor vendor = new Vendor();
        vendor.setId(UUID.randomUUID());
        vendor.setOrganization(organization);
        vendor.setVendorCode("VEN-2026-001");
        vendor.setCompanyName("Acme Supplies");
        vendor.setEmail("sales@acme.test");
        vendor.setStatus(VendorStatus.PROSPECTIVE);
        vendor.setRating(BigDecimal.ZERO.setScale(2));
        vendor.setRegisteredAt(NOW);
        return vendor;
    }
}
