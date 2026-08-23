package com.vendorsphere.vendor.service;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.service.AuditService;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.common.util.Money;
import com.vendorsphere.common.util.ReferenceNumberGenerator;
import com.vendorsphere.common.util.ReferencePrefix;
import com.vendorsphere.organization.repository.OrganizationRepository;
import com.vendorsphere.vendor.VendorStatus;
import com.vendorsphere.vendor.dto.VendorProfileSnapshot;
import com.vendorsphere.vendor.dto.VendorRequest;
import com.vendorsphere.vendor.dto.VendorResponse;
import com.vendorsphere.vendor.entity.Vendor;
import com.vendorsphere.vendor.entity.VendorCategory;
import com.vendorsphere.vendor.repository.VendorCategoryRepository;
import com.vendorsphere.vendor.repository.VendorDocumentRepository;
import com.vendorsphere.vendor.repository.VendorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Vendor registration, profile update and detail read (Requirement 2).
 *
 * <p>Status transitions, contacts, categories, documents and paged search live in their own
 * services; this one owns the profile itself and the three derived figures a detail read carries.
 *
 * <p>Every read is keyed on the caller's organization, so a vendor identifier belonging to another
 * tenant simply misses and surfaces as 404 {@code Vendor not found} rather than 403, which keeps
 * identifiers unenumerable (Requirements 2.6, 30.10).
 */
@Service
public class VendorService {

    /** Pinned by Requirement 2.3, so it is a constant rather than a literal at two call sites. */
    static final String DUPLICATE_EMAIL_MESSAGE = "Vendor email already registered";

    /** Pinned by Requirement 2.6. */
    static final String NOT_FOUND_MESSAGE = "Vendor not found";

    /** Requirement 2.5 counts documents expiring within 30 days of the request date. */
    static final int EXPIRY_WINDOW_DAYS = 30;

    /**
     * Converts a stored 0.00–5.00 vendor rating back to a 0.00–100.00 performance score. It is the
     * inverse of the {@code score / 20} rating the performance engine derives, so the two figures
     * stay consistent while no snapshot exists.
     */
    private static final BigDecimal RATING_TO_SCORE_FACTOR = new BigDecimal("20");

    private final VendorRepository vendorRepository;
    private final VendorCategoryRepository vendorCategoryRepository;
    private final VendorDocumentRepository vendorDocumentRepository;
    private final OrganizationRepository organizationRepository;
    private final ReferenceNumberGenerator referenceNumberGenerator;
    private final AuditService auditService;
    private final Clock clock;

    public VendorService(
            VendorRepository vendorRepository,
            VendorCategoryRepository vendorCategoryRepository,
            VendorDocumentRepository vendorDocumentRepository,
            OrganizationRepository organizationRepository,
            ReferenceNumberGenerator referenceNumberGenerator,
            AuditService auditService,
            Clock clock
    ) {
        this.vendorRepository = vendorRepository;
        this.vendorCategoryRepository = vendorCategoryRepository;
        this.vendorDocumentRepository = vendorDocumentRepository;
        this.organizationRepository = organizationRepository;
        this.referenceNumberGenerator = referenceNumberGenerator;
        this.auditService = auditService;
        this.clock = clock;
    }

    /**
     * Registers a vendor for the caller's organization with status {@code PROSPECTIVE}, rating
     * {@code 0.00}, a generated {@code VEN} code and a registration timestamp set to the creation
     * instant (Requirement 2.1).
     *
     * <p>Email uniqueness is per organization, exactly as Requirement 2.3 scopes it: two tenants may
     * each register the same supplier address, and a repeat within one tenant is rejected with 409
     * {@code Vendor email already registered}. The comparison is case-insensitive, so a differently
     * cased duplicate is still a duplicate.
     *
     * <p>The vendor code is allocated on this transaction, so it is consumed exactly when the vendor
     * row commits and released when the transaction rolls back (Requirement 1.5).
     */
    @Transactional
    public VendorResponse register(VendorRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        assertEmailAvailable(organizationId, request.email());

        Vendor vendor = new Vendor();
        vendor.setOrganization(organizationRepository.getReferenceById(organizationId));
        vendor.setVendorCode(referenceNumberGenerator.allocate(organizationId, ReferencePrefix.VEN));
        vendor.setStatus(VendorStatus.PROSPECTIVE);
        vendor.setRating(Money.money(BigDecimal.ZERO));
        vendor.setRegisteredAt(clock.instant());
        applyProfile(vendor, request, organizationId);

        Vendor saved = vendorRepository.save(vendor);
        auditService.record(AuditAction.VENDOR_CREATED, "Vendor", saved.getId(),
                null, VendorProfileSnapshot.from(saved));
        return toResponse(saved, organizationId);
    }

    /**
     * Applies the supplied profile fields to a vendor, leaving status and rating untouched
     * (Requirement 2.4).
     *
     * <p>The duplicate-email guard runs only when the address actually changes: re-submitting the
     * vendor's own address is not a clash, while moving to an address another vendor of the same
     * organization already holds is, and is rejected with the same 409 as registration so the
     * per-organization uniqueness Requirement 2.3 establishes cannot be broken by an update.
     */
    @Transactional
    public VendorResponse update(UUID vendorId, VendorRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Vendor vendor = findInOrganization(vendorId, organizationId);
        VendorProfileSnapshot previous = VendorProfileSnapshot.from(vendor);

        if (!vendor.getEmail().equalsIgnoreCase(request.email())) {
            assertEmailAvailable(organizationId, request.email());
        }
        applyProfile(vendor, request, organizationId);

        Vendor saved = vendorRepository.save(vendor);
        auditService.record(AuditAction.VENDOR_UPDATED, "Vendor", saved.getId(),
                previous, VendorProfileSnapshot.from(saved));
        return toResponse(saved, organizationId);
    }

    /**
     * Returns the vendor profile with its category name, current performance score and count of
     * documents expiring within 30 days of the request date (Requirement 2.5).
     */
    @Transactional(readOnly = true)
    public VendorResponse get(UUID vendorId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return toResponse(findInOrganization(vendorId, organizationId), organizationId);
    }

    private Vendor findInOrganization(UUID vendorId, UUID organizationId) {
        return vendorRepository.findByIdAndOrganizationId(vendorId, organizationId)
                .orElseThrow(() -> new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
    }

    private void assertEmailAvailable(UUID organizationId, String email) {
        if (vendorRepository.existsByOrganizationIdAndEmailIgnoreCase(organizationId, email)) {
            throw new BusinessException(DUPLICATE_EMAIL_MESSAGE, HttpStatus.CONFLICT);
        }
    }

    private void applyProfile(Vendor vendor, VendorRequest request, UUID organizationId) {
        vendor.setCompanyName(request.companyName());
        vendor.setContactPerson(request.contactPerson());
        vendor.setEmail(request.email());
        vendor.setPhone(request.phone());
        vendor.setAddress(request.address());
        vendor.setTaxIdentifier(request.taxIdentifier());
        vendor.setCategory(resolveCategory(request.categoryId(), organizationId));
    }

    /**
     * Resolves the vendor category within the caller's organization. A category of another tenant
     * misses the tenant-scoped finder and is reported as not found rather than forbidden, for the
     * same reason vendor identifiers are (Requirement 30.10).
     */
    private VendorCategory resolveCategory(UUID categoryId, UUID organizationId) {
        if (categoryId == null) {
            return null;
        }
        return vendorCategoryRepository.findByIdAndOrganizationId(categoryId, organizationId)
                .orElseThrow(() -> new BusinessException(
                        "Vendor category not found", HttpStatus.NOT_FOUND));
    }

    private VendorResponse toResponse(Vendor vendor, UUID organizationId) {
        return VendorResponse.from(
                vendor,
                performanceScoreOf(vendor),
                expiringDocumentCountOf(vendor, organizationId));
    }

    /**
     * The vendor's current Performance_Score.
     *
     * <p>The most recent {@code vendor_performance_snapshots} row is authoritative once the
     * performance module writes one. Until then no snapshot exists, so the score is derived from the
     * stored rating by the inverse of the engine's {@code score / 20} mapping. A vendor registered by
     * {@link #register} therefore reports {@code 0.00}, which is its true score rather than a
     * placeholder, and the figure starts tracking real metrics the moment snapshots appear.
     */
    private BigDecimal performanceScoreOf(Vendor vendor) {
        return vendorRepository.findLatestPerformanceScore(vendor.getId())
                .map(Money::clampScore)
                .orElseGet(() -> Money.clampScore(
                        Money.multiply(vendor.getRating(), RATING_TO_SCORE_FACTOR)));
    }

    private long expiringDocumentCountOf(Vendor vendor, UUID organizationId) {
        LocalDate today = LocalDate.now(clock);
        return vendorDocumentRepository.countByVendorIdAndVendorOrganizationIdAndExpiryDateBetween(
                vendor.getId(), organizationId, today, today.plusDays(EXPIRY_WINDOW_DAYS));
    }
}
