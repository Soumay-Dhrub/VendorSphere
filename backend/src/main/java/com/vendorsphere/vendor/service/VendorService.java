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

@Service
public class VendorService {

    static final String DUPLICATE_EMAIL_MESSAGE = "Vendor email already registered";

    static final String NOT_FOUND_MESSAGE = "Vendor not found";

    static final int EXPIRY_WINDOW_DAYS = DocumentExpiryEvaluator.EXPIRING_SOON_WINDOW_DAYS;

    public static final SortWhitelist SORTABLE =
            SortWhitelist.of("companyName", "registeredAt", "rating", "status");

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

    @Transactional(readOnly = true)
    public VendorResponse get(UUID vendorId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return toResponse(findVisibleVendor(vendorId, organizationId), organizationId);
    }

    @Transactional(readOnly = true)
    public VendorPerformanceResponse performance(UUID vendorId) {
        Vendor vendor = findVisibleVendor(
                vendorId, SecurityUtils.getCurrentOrganizationId());
        return VendorPerformanceResponse.from(
                vendor.getId(), performanceScoreOf(vendor), Money.money(vendor.getRating()));
    }

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

    private BigDecimal performanceScoreOf(Vendor vendor) {
        return performanceScore(vendor, vendorRepository.findLatestPerformanceScore(vendor.getId())
                .orElse(null));
    }

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
