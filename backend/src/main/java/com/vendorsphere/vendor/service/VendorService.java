package com.vendorsphere.vendor.service;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.service.AuditService;
import com.vendorsphere.common.dto.PageResponse;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.common.util.Money;
import com.vendorsphere.common.util.PageSupport;
import com.vendorsphere.common.util.ReferenceNumberGenerator;
import com.vendorsphere.common.util.ReferencePrefix;
import com.vendorsphere.common.util.SortWhitelist;
import com.vendorsphere.organization.repository.OrganizationRepository;
import com.vendorsphere.vendor.DocumentExpiryEvaluator;
import com.vendorsphere.vendor.VendorStatus;
import com.vendorsphere.vendor.dto.VendorPerformanceResponse;
import com.vendorsphere.vendor.dto.VendorProfileSnapshot;
import com.vendorsphere.vendor.dto.VendorRequest;
import com.vendorsphere.vendor.dto.VendorResponse;
import com.vendorsphere.vendor.dto.VendorSearchCriteria;
import com.vendorsphere.vendor.entity.Vendor;
import com.vendorsphere.vendor.entity.VendorCategory;
import com.vendorsphere.vendor.repository.VendorCategoryRepository;
import com.vendorsphere.vendor.repository.VendorDocumentRepository;
import com.vendorsphere.vendor.repository.VendorRepository;
import com.vendorsphere.vendor.repository.VendorSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    /**
     * Requirement 2.5 counts documents expiring within 30 days of the request date, which is the
     * same window {@link DocumentExpiryEvaluator} classifies as EXPIRING_SOON. It is taken from
     * there rather than restated, so the counted set and the listed state cannot drift apart.
     */
    static final int EXPIRY_WINDOW_DAYS = DocumentExpiryEvaluator.EXPIRING_SOON_WINDOW_DAYS;

    /**
     * The four sortable fields of the vendor listing (Requirement 6.7), as entity attribute names.
     *
     * <p>Company name is the default: an unsorted vendor list is read alphabetically far more often
     * than by registration order, and it is the first field Requirement 6.7 names. Anything outside
     * this set is rejected by {@code PageSupport} with 400 listing these four (Requirement 31.5).
     *
     * <p>Published so the controller of task 5.10 shares one definition with the service rather than
     * restating it.
     */
    public static final SortWhitelist SORTABLE =
            SortWhitelist.of("companyName", "registeredAt", "rating", "status");

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
    private final VendorAccessGuard vendorAccessGuard;
    private final Clock clock;

    public VendorService(
            VendorRepository vendorRepository,
            VendorCategoryRepository vendorCategoryRepository,
            VendorDocumentRepository vendorDocumentRepository,
            OrganizationRepository organizationRepository,
            ReferenceNumberGenerator referenceNumberGenerator,
            AuditService auditService,
            VendorAccessGuard vendorAccessGuard,
            Clock clock
    ) {
        this.vendorRepository = vendorRepository;
        this.vendorCategoryRepository = vendorCategoryRepository;
        this.vendorDocumentRepository = vendorDocumentRepository;
        this.organizationRepository = organizationRepository;
        this.referenceNumberGenerator = referenceNumberGenerator;
        this.auditService = auditService;
        this.vendorAccessGuard = vendorAccessGuard;
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
        Vendor vendor = findVisibleVendor(vendorId, organizationId);
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
        return toResponse(findVisibleVendor(vendorId, organizationId), organizationId);
    }

    /**
     * The vendor's current performance standing: its Performance_Score and the rating derived from
     * it, both resolved exactly as a detail read resolves them (Requirement 2.5).
     */
    @Transactional(readOnly = true)
    public VendorPerformanceResponse performance(UUID vendorId) {
        Vendor vendor = findVisibleVendor(
                vendorId, SecurityUtils.getCurrentOrganizationId());
        return VendorPerformanceResponse.from(
                vendor.getId(), performanceScoreOf(vendor), Money.money(vendor.getRating()));
    }

    /**
     * Returns a page of the caller's organization's vendors, narrowed by whichever of the four
     * optional filters were supplied (Requirements 6.1 through 6.6).
     *
     * <p>Paging defaults, the size clamp and the sort allowlist all belong to
     * {@link com.vendorsphere.common.util.PageSupport}, which the caller uses with {@link #SORTABLE}
     * to build the {@code pageable}; nothing about them is reimplemented here.
     *
     * <h4>Why the derived figures are batched</h4>
     *
     * <p>Each row carries a performance score and an expiring-document count, exactly as a detail read
     * does. Deriving them the way {@link #get(UUID)} does would issue two queries per vendor, so a
     * page of 20 would cost 40 extra round trips and a page of 100 would cost 200 — cost growing with
     * page size, which Requirement 31.2 does not tolerate for an endpoint that accepts {@code size} up
     * to 100. Instead both figures are read once for the whole page, keyed on the page's vendor
     * identifiers, and the category is left-join fetched by the specification. A page therefore costs
     * four queries (content, count, scores, document counts) whatever its size.
     */
    @Transactional(readOnly = true)
    public PageResponse<VendorResponse> search(VendorSearchCriteria criteria, Pageable pageable) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Page<Vendor> page = vendorRepository.findAll(
                VendorSpecifications.search(organizationId, criteria), pageable);

        List<UUID> vendorIds = page.getContent().stream().map(Vendor::getId).toList();
        Map<UUID, BigDecimal> snapshotScores =
                vendorRepository.latestPerformanceScoresByVendorId(vendorIds);
        LocalDate today = LocalDate.now(clock);
        Map<UUID, Long> expiringCounts = vendorDocumentRepository.expiringDocumentCountsByVendorId(
                vendorIds, organizationId, today, today.plusDays(EXPIRY_WINDOW_DAYS));

        return PageSupport.map(page, vendor -> VendorResponse.from(
                vendor,
                performanceScore(vendor, snapshotScores.get(vendor.getId())),
                expiringCounts.getOrDefault(vendor.getId(), 0L)));
    }

    /**
     * Loads a vendor within the caller's organization, then applies the vendor-user restriction of
     * Requirement 2.7: an internal user passes untouched, while a caller holding the VENDOR role is
     * denied any profile other than the one its account is linked to. The guard raises 404 with this
     * service's pinned wording, so a vendor user cannot tell another vendor's profile apart from a
     * missing one (Requirements 2.6, 30.10).
     */
    private Vendor findVisibleVendor(UUID vendorId, UUID organizationId) {
        Vendor vendor = findInOrganization(vendorId, organizationId);
        vendorAccessGuard.assertVendorVisible(vendor.getId(), NOT_FOUND_MESSAGE);
        return vendor;
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
        return performanceScore(vendor, vendorRepository.findLatestPerformanceScore(vendor.getId())
                .orElse(null));
    }

    /**
     * The score rule of {@link #performanceScoreOf(Vendor)} applied to an already-read snapshot score,
     * so a detail read and a list row derive the figure identically and only the way the snapshot is
     * fetched differs.
     *
     * @param snapshotScore the latest snapshot score, or {@code null} when the vendor has no snapshot
     */
    private BigDecimal performanceScore(Vendor vendor, BigDecimal snapshotScore) {
        return snapshotScore != null
                ? Money.clampScore(snapshotScore)
                : Money.clampScore(Money.multiply(vendor.getRating(), RATING_TO_SCORE_FACTOR));
    }

    private long expiringDocumentCountOf(Vendor vendor, UUID organizationId) {
        LocalDate today = LocalDate.now(clock);
        return vendorDocumentRepository.countByVendorIdAndVendorOrganizationIdAndExpiryDateBetween(
                vendor.getId(), organizationId, today, today.plusDays(EXPIRY_WINDOW_DAYS));
    }
}
