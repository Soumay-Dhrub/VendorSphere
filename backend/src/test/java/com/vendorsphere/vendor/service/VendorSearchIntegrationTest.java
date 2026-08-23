package com.vendorsphere.vendor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.vendorsphere.auth.security.UserPrincipal;
import com.vendorsphere.common.dto.PageResponse;
import com.vendorsphere.common.util.PageSupport;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.testsupport.AbstractIntegrationTest;
import com.vendorsphere.vendor.VendorDocumentType;
import com.vendorsphere.vendor.VendorStatus;
import com.vendorsphere.vendor.dto.VendorResponse;
import com.vendorsphere.vendor.dto.VendorSearchCriteria;
import com.vendorsphere.vendor.entity.Vendor;
import com.vendorsphere.vendor.entity.VendorCategory;
import com.vendorsphere.vendor.entity.VendorDocument;
import com.vendorsphere.vendor.repository.VendorCategoryRepository;
import com.vendorsphere.vendor.repository.VendorDocumentRepository;
import com.vendorsphere.vendor.repository.VendorRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exercises the vendor listing of Requirement 6 against PostgreSQL, because what is under test is
 * the SQL a specification produces: a case-insensitive {@code LIKE}, an inclusive rating bound, the
 * conjunction of several filters (Requirement 6.6), the tenant predicate that no criteria value can
 * bypass, and ordering by each of the four sortable fields (Requirement 6.7).
 *
 * <p>Every test is transactional, so it rolls back on the shared database, and every fixture lives in
 * a freshly generated organization, so the assertions below are exact rather than deltas.
 *
 * <p>Company names are chosen so that the character which decides their order is upper case in both
 * spellings ({@code ACME Instruments} before {@code Acme Logistics} on {@code I} < {@code L}). The
 * expected ordering is therefore the same under a C and a locale-aware collation, and the sort
 * assertions do not depend on the container's default collation.
 */
@Transactional
class VendorSearchIntegrationTest extends AbstractIntegrationTest {

    private static final Instant REGISTERED_BASE = Instant.parse("2026-01-05T08:00:00Z");

    @Autowired
    private VendorService vendorService;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private VendorCategoryRepository vendorCategoryRepository;

    @Autowired
    private VendorDocumentRepository vendorDocumentRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Organization organization;
    private VendorCategory hardware;
    private VendorCategory logistics;
    private Vendor acmeLogistics;
    private Vendor acmeSupplies;
    private Vendor foreignVendor;
    private UUID foreignOrganizationId;
    private VendorCategory foreignCategory;

    @BeforeEach
    void seedTwoOrganizations() {
        TestActor actor = newActor("PROCUREMENT_OFFICER");
        organization = organizationRepository.findById(actor.organizationId()).orElseThrow();
        authenticate(actor);

        hardware = category(organization, "Hardware");
        logistics = category(organization, "Logistics");

        // Ratings and registration instants are all distinct, so every sort has one correct answer.
        acmeSupplies = vendor("Acme Supplies", hardware, VendorStatus.ACTIVE, "4.50", 0);
        acmeLogistics = vendor("Acme Logistics", logistics, VendorStatus.ACTIVE, "3.00", 1);
        vendor("Beta Hardware", hardware, VendorStatus.SUSPENDED, "4.80", 2);
        vendor("ACME Instruments", hardware, VendorStatus.ACTIVE, "4.10", 3);

        Organization other = newOrganization("vendor-foreign");
        foreignOrganizationId = other.getId();
        foreignCategory = category(other, "Hardware");
        foreignVendor = vendorOf(other, "Acme Foreign", foreignCategory, VendorStatus.ACTIVE, "5.00", 4);
        entityManager.flush();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /** Requirements 6.1 and 31.1: a page of the caller's organization only, in the PageResponse shape. */
    @Test
    void listsEveryVendorOfTheCallersOrganizationAndNoVendorOfAnother() {
        PageResponse<VendorResponse> page = search(VendorSearchCriteria.none(), pageable(null, null));

        assertThat(page.content()).extracting(VendorResponse::companyName)
                .containsExactly("ACME Instruments", "Acme Logistics", "Acme Supplies", "Beta Hardware");
        assertThat(page.content()).extracting(VendorResponse::id)
                .doesNotContain(foreignVendor.getId());
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(PageSupport.DEFAULT_SIZE);
        assertThat(page.totalElements()).isEqualTo(4);
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.first()).isTrue();
        assertThat(page.last()).isTrue();
        // Requirement 6.1: the category name travels with each row.
        assertThat(page.content()).extracting(VendorResponse::categoryName)
                .containsExactly("Hardware", "Logistics", "Hardware", "Hardware");
    }

    /** Requirement 6.2. */
    @Test
    void companyNameFilterMatchesACaseInsensitiveSubstring() {
        assertThat(names(search(criteria("acme", null, null, null), pageable(null, null))))
                .containsExactly("ACME Instruments", "Acme Logistics", "Acme Supplies");
        assertThat(names(search(criteria("SUPPL", null, null, null), pageable(null, null))))
                .containsExactly("Acme Supplies");
        assertThat(names(search(criteria("  acme  ", null, null, null), pageable(null, null))))
                .containsExactly("ACME Instruments", "Acme Logistics", "Acme Supplies");
        // A blank term is not a filter, so it narrows nothing.
        assertThat(names(search(criteria("   ", null, null, null), pageable(null, null)))).hasSize(4);
        // Wildcards in the term are matched literally rather than expanded.
        assertThat(names(search(criteria("%", null, null, null), pageable(null, null)))).isEmpty();
        assertThat(names(search(criteria("Acme_", null, null, null), pageable(null, null)))).isEmpty();
    }

    /** Requirements 6.3, 6.4 and 6.5, each filter on its own. */
    @Test
    void categoryStatusAndMinimumRatingFilterIndependently() {
        assertThat(names(search(criteria(null, hardware.getId(), null, null), pageable(null, null))))
                .containsExactly("ACME Instruments", "Acme Supplies", "Beta Hardware");
        assertThat(names(search(criteria(null, null, VendorStatus.SUSPENDED, null), pageable(null, null))))
                .containsExactly("Beta Hardware");
        assertThat(names(search(criteria(null, null, null, new BigDecimal("4.50")), pageable(null, null))))
                .containsExactly("Acme Supplies", "Beta Hardware");
        // The rating bound is inclusive at both a matching and a non-matching neighbour.
        assertThat(names(search(criteria(null, null, null, new BigDecimal("4.80")), pageable(null, null))))
                .containsExactly("Beta Hardware");
        assertThat(names(search(criteria(null, null, null, new BigDecimal("4.81")), pageable(null, null))))
                .isEmpty();
    }

    /** Requirement 6.6: supplied filters combine, so a vendor must satisfy every one of them. */
    @Test
    void combinedFiltersReturnOnlyVendorsSatisfyingEveryFilter() {
        VendorSearchCriteria allFour =
                criteria("acme", hardware.getId(), VendorStatus.ACTIVE, new BigDecimal("4.10"));
        assertThat(names(search(allFour, pageable(null, null))))
                .containsExactly("ACME Instruments", "Acme Supplies");

        // Each filter alone would admit more; tightening any single one narrows the conjunction.
        assertThat(names(search(
                criteria("acme", hardware.getId(), VendorStatus.ACTIVE, new BigDecimal("4.50")),
                pageable(null, null))))
                .containsExactly("Acme Supplies");
        // Beta Hardware is the only SUSPENDED vendor, but its name does not contain the term.
        assertThat(names(search(
                criteria("acme", hardware.getId(), VendorStatus.SUSPENDED, null), pageable(null, null))))
                .isEmpty();
        // Acme Logistics matches the term, the status and the category of no interest here.
        assertThat(names(search(
                criteria("acme", logistics.getId(), VendorStatus.ACTIVE, null), pageable(null, null))))
                .containsExactly("Acme Logistics");
        assertThat(names(search(
                criteria("acme", logistics.getId(), VendorStatus.ACTIVE, new BigDecimal("3.01")),
                pageable(null, null))))
                .isEmpty();
    }

    /** Requirement 30.10: no combination of criteria values reaches another organization's vendors. */
    @Test
    void theOrganizationRestrictionCannotBeBypassedByAnyCriteriaValue() {
        // Criteria that describe the foreign vendor exactly, including its own category identifier.
        assertThat(search(
                criteria("Acme Foreign", foreignCategory.getId(), VendorStatus.ACTIVE,
                        new BigDecimal("5.00")),
                pageable(null, null)).content())
                .isEmpty();
        // A foreign category identifier on its own narrows to nothing rather than widening the tenant.
        assertThat(search(criteria(null, foreignCategory.getId(), null, null), pageable(null, null))
                .content())
                .isEmpty();
        // The foreign vendor is nonetheless there to be found by its owner.
        assertThat(vendorRepository.findByIdAndOrganizationId(
                foreignVendor.getId(), foreignOrganizationId)).isPresent();
    }

    /** Requirement 6.7: all four fields sort, in both directions. */
    @Test
    void sortsByCompanyNameRegistrationTimestampRatingAndStatus() {
        assertThat(names(search(VendorSearchCriteria.none(), pageable("companyName", "ASC"))))
                .containsExactly("ACME Instruments", "Acme Logistics", "Acme Supplies", "Beta Hardware");
        assertThat(names(search(VendorSearchCriteria.none(), pageable("companyName", "DESC"))))
                .containsExactly("Beta Hardware", "Acme Supplies", "Acme Logistics", "ACME Instruments");
        assertThat(names(search(VendorSearchCriteria.none(), pageable("registeredAt", "ASC"))))
                .containsExactly("Acme Supplies", "Acme Logistics", "Beta Hardware", "ACME Instruments");
        assertThat(names(search(VendorSearchCriteria.none(), pageable("registeredAt", "DESC"))))
                .containsExactly("ACME Instruments", "Beta Hardware", "Acme Logistics", "Acme Supplies");
        assertThat(names(search(VendorSearchCriteria.none(), pageable("rating", "DESC"))))
                .containsExactly("Beta Hardware", "Acme Supplies", "ACME Instruments", "Acme Logistics");
        assertThat(search(VendorSearchCriteria.none(), pageable("status", "ASC")).content())
                .extracting(VendorResponse::status)
                .containsExactly(VendorStatus.ACTIVE, VendorStatus.ACTIVE, VendorStatus.ACTIVE,
                        VendorStatus.SUSPENDED);
        assertThat(search(VendorSearchCriteria.none(), pageable("status", "DESC")).content())
                .extracting(VendorResponse::status)
                .startsWith(VendorStatus.SUSPENDED);
    }

    /** Requirement 31.1: the page metadata reflects the filtered total, not the whole table. */
    @Test
    void pagesTheFilteredResult() {
        PageResponse<VendorResponse> firstPage = search(criteria("acme", null, null, null),
                PageSupport.pageable(0, 2, "companyName", "ASC", VendorService.SORTABLE));

        assertThat(firstPage.content()).extracting(VendorResponse::companyName)
                .containsExactly("ACME Instruments", "Acme Logistics");
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.first()).isTrue();
        assertThat(firstPage.last()).isFalse();

        PageResponse<VendorResponse> secondPage = search(criteria("acme", null, null, null),
                PageSupport.pageable(1, 2, "companyName", "ASC", VendorService.SORTABLE));
        assertThat(names(secondPage)).containsExactly("Acme Supplies");
        assertThat(secondPage.last()).isTrue();
    }

    /**
     * Requirements 2.5 and 31.2: each row carries the same two derived figures a detail read carries,
     * and both are read for the whole page at once. The snapshot pair proves the batch query resolves
     * the latest snapshot per vendor exactly as the single-vendor query does.
     */
    @Test
    void derivesThePerformanceScoreAndExpiringDocumentCountForEveryRowOfThePage() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        snapshot(acmeSupplies, today.minusMonths(2), today.minusMonths(1), "55.00");
        snapshot(acmeSupplies, today.minusMonths(1), today, "72.40");

        document(acmeLogistics, today.plusDays(1));    // inside the 30 day window
        document(acmeLogistics, today.plusDays(30));   // inclusive upper bound
        document(acmeLogistics, today.minusDays(1));   // already expired
        document(acmeLogistics, today.plusDays(31));   // outside the window
        document(acmeLogistics, null);                 // no expiry date at all
        entityManager.flush();

        PageResponse<VendorResponse> page = search(VendorSearchCriteria.none(), pageable(null, null));

        assertThat(page.content()).extracting(VendorResponse::companyName, VendorResponse::performanceScore)
                .containsExactly(
                        // No snapshot, so the score is the inverse of the score / 20 rating mapping.
                        tuple("ACME Instruments", new BigDecimal("82.00")),
                        tuple("Acme Logistics", new BigDecimal("60.00")),
                        // The later of the vendor's two snapshots wins.
                        tuple("Acme Supplies", new BigDecimal("72.40")),
                        tuple("Beta Hardware", new BigDecimal("96.00")));
        assertThat(page.content())
                .extracting(VendorResponse::companyName, VendorResponse::expiringDocumentCount)
                .containsExactly(
                        tuple("ACME Instruments", 0L),
                        tuple("Acme Logistics", 2L),
                        tuple("Acme Supplies", 0L),
                        tuple("Beta Hardware", 0L));
    }

    /** An empty page derives nothing and still reports the PageResponse shape. */
    @Test
    void anEmptyPageIsWellFormed() {
        PageResponse<VendorResponse> page =
                search(criteria("no such vendor", null, null, null), pageable(null, null));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
        assertThat(page.first()).isTrue();
        assertThat(page.last()).isTrue();
    }

    // ----- helpers -----

    private PageResponse<VendorResponse> search(VendorSearchCriteria criteria, Pageable pageable) {
        entityManager.flush();
        entityManager.clear();
        return vendorService.search(criteria, pageable);
    }

    private static Pageable pageable(String sort, String direction) {
        return PageSupport.pageable(null, null, sort, direction, VendorService.SORTABLE);
    }

    private static VendorSearchCriteria criteria(
            String companyName, UUID categoryId, VendorStatus status, BigDecimal minRating) {
        return new VendorSearchCriteria(companyName, categoryId, status, minRating);
    }

    private static List<String> names(PageResponse<VendorResponse> page) {
        return page.content().stream().map(VendorResponse::companyName).toList();
    }

    private void authenticate(TestActor actor) {
        UserPrincipal principal = principalOf(actor.email());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    // ----- fixtures -----

    private VendorCategory category(Organization owner, String name) {
        VendorCategory category = new VendorCategory();
        category.setOrganization(owner);
        category.setName(name);
        category.setDescription(name + " suppliers");
        return vendorCategoryRepository.saveAndFlush(category);
    }

    private Vendor vendor(
            String companyName, VendorCategory category, VendorStatus status, String rating, int dayOffset) {
        return vendorOf(organization, companyName, category, status, rating, dayOffset);
    }

    private Vendor vendorOf(
            Organization owner,
            String companyName,
            VendorCategory category,
            VendorStatus status,
            String rating,
            int dayOffset) {
        Vendor vendor = new Vendor();
        vendor.setOrganization(owner);
        vendor.setCategory(category);
        vendor.setVendorCode("VEN-2026-" + UUID.randomUUID());
        vendor.setCompanyName(companyName);
        vendor.setEmail("contact-" + UUID.randomUUID() + "@example.test");
        vendor.setStatus(status);
        vendor.setRating(new BigDecimal(rating));
        vendor.setRegisteredAt(REGISTERED_BASE.plus(dayOffset, ChronoUnit.DAYS));
        return vendorRepository.saveAndFlush(vendor);
    }

    private void document(Vendor vendor, LocalDate expiryDate) {
        VendorDocument document = new VendorDocument();
        document.setVendor(vendor);
        document.setDocumentType(VendorDocumentType.GST_CERTIFICATE);
        document.setFileName("gst.pdf");
        document.setFileUrl("attachment:" + UUID.randomUUID());
        document.setExpiryDate(expiryDate);
        document.setUploadedAt(Instant.now());
        vendorDocumentRepository.save(document);
    }

    /**
     * Inserts a performance snapshot with SQL, because {@code vendor_performance_snapshots} has no
     * entity yet: its mapping belongs to the performance module, and the listing only reads one column.
     */
    private void snapshot(Vendor vendor, LocalDate periodStart, LocalDate periodEnd, String overall) {
        entityManager.flush();
        jdbcTemplate.update("""
                        INSERT INTO vendor_performance_snapshots
                            (vendor_id, organization_id, period_start, period_end, overall_score)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                vendor.getId(),
                organization.getId(),
                Date.valueOf(periodStart),
                Date.valueOf(periodEnd),
                new BigDecimal(overall));
    }
}
