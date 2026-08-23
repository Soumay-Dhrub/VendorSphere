package com.vendorsphere.vendor.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.organization.repository.OrganizationRepository;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.user.repository.UserRepository;
import com.vendorsphere.vendor.VendorDocumentType;
import com.vendorsphere.vendor.VendorStatus;
import com.vendorsphere.vendor.entity.Vendor;
import com.vendorsphere.vendor.entity.VendorCategory;
import com.vendorsphere.vendor.entity.VendorContact;
import com.vendorsphere.vendor.entity.VendorDocument;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises the vendor mappings and finders against PostgreSQL, because the three things under test
 * here are database behaviour a mock cannot show: the round trip of every mapped column, the
 * optimistic-lock column added by V2 (Requirement 32.3), and tenant-scoped reads that must miss on a
 * cross-tenant identifier (Requirement 30.10).
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class VendorRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private VendorCategoryRepository vendorCategoryRepository;

    @Autowired
    private VendorContactRepository vendorContactRepository;

    @Autowired
    private VendorDocumentRepository vendorDocumentRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void roundTripsTheVendorProfileIncludingCategoryAndLinkedPortalUser() {
        Organization organization = organization();
        VendorCategory category = category(organization, "Hardware");
        User portalUser = user(organization);

        Vendor vendor = vendor(organization, "VEN-2026-001", "Acme Supplies");
        vendor.setCategory(category);
        vendor.setUser(portalUser);
        vendor.setContactPerson("Riya Nair");
        vendor.setPhone("+91-9000000000");
        vendor.setAddress("12 Industrial Estate");
        vendor.setTaxIdentifier("29ABCDE1234F1Z5");
        vendor.setRating(new BigDecimal("4.25"));
        UUID vendorId = vendorRepository.saveAndFlush(vendor).getId();
        entityManager.clear();

        Vendor reloaded = vendorRepository.findByIdAndOrganizationId(vendorId, organization.getId())
                .orElseThrow();

        assertThat(reloaded.getVendorCode()).isEqualTo("VEN-2026-001");
        assertThat(reloaded.getCompanyName()).isEqualTo("Acme Supplies");
        assertThat(reloaded.getEmail()).isEqualTo(vendor.getEmail());
        assertThat(reloaded.getContactPerson()).isEqualTo("Riya Nair");
        assertThat(reloaded.getPhone()).isEqualTo("+91-9000000000");
        assertThat(reloaded.getAddress()).isEqualTo("12 Industrial Estate");
        assertThat(reloaded.getTaxIdentifier()).isEqualTo("29ABCDE1234F1Z5");
        assertThat(reloaded.getStatus()).isEqualTo(VendorStatus.PROSPECTIVE);
        assertThat(reloaded.getRating()).isEqualByComparingTo("4.25");
        assertThat(reloaded.getRegisteredAt()).isNotNull();
        assertThat(reloaded.getCategory().getId()).isEqualTo(category.getId());
        assertThat(reloaded.getUser().getId()).isEqualTo(portalUser.getId());
    }

    @Test
    @Transactional
    void versionAdvancesOnEveryUpdateSoConcurrentEditsCanBeDetected() {
        Organization organization = organization();
        Vendor vendor = vendorRepository.saveAndFlush(
                vendor(organization, "VEN-2026-002", "Beta Traders"));

        assertThat(vendor.getVersion()).isZero();

        vendor.setStatus(VendorStatus.ACTIVE);
        vendor.setStatusChangeReason("Qualified after site visit");
        vendorRepository.saveAndFlush(vendor);

        assertThat(vendor.getVersion()).isEqualTo(1L);
        assertThat(vendor.getStatusChangeReason()).isEqualTo("Qualified after site visit");
    }

    @Test
    @Transactional
    void vendorAndCategoryReadsMissOnACrossTenantIdentifier() {
        Organization mine = organization();
        Organization theirs = organization();
        Vendor theirVendor = vendorRepository.saveAndFlush(
                vendor(theirs, "VEN-2026-003", "Gamma Metals"));
        VendorCategory theirCategory = category(theirs, "Consumables");
        entityManager.flush();

        assertThat(vendorRepository.findByIdAndOrganizationId(theirVendor.getId(), mine.getId()))
                .isEmpty();
        assertThat(vendorRepository.findByIdAndOrganizationId(theirVendor.getId(), theirs.getId()))
                .isPresent();
        assertThat(vendorCategoryRepository.findByIdAndOrganizationId(
                theirCategory.getId(), mine.getId())).isEmpty();
        assertThat(vendorCategoryRepository.findByOrganizationIdOrderByNameAsc(mine.getId()))
                .isEmpty();
        assertThat(vendorCategoryRepository.existsByOrganizationIdAndNameIgnoreCase(
                theirs.getId(), "consumables")).isTrue();
        assertThat(vendorCategoryRepository.existsByOrganizationIdAndNameIgnoreCase(
                mine.getId(), "consumables")).isFalse();
        assertThat(vendorRepository.existsByOrganizationIdAndEmailIgnoreCase(
                theirs.getId(), theirVendor.getEmail().toUpperCase())).isTrue();
        assertThat(vendorRepository.existsByOrganizationIdAndEmailIgnoreCase(
                mine.getId(), theirVendor.getEmail())).isFalse();
    }

    /**
     * The finder the vendor-scoped access guard resolves a caller through (Requirements 2.7, 30.8).
     * It is organization-keyed like every other read, so a portal user can never resolve to a vendor
     * of another tenant (Requirement 30.10).
     */
    @Test
    @Transactional
    void resolvesTheVendorLinkedToAPortalUserOnlyWithinItsOwnOrganization() {
        Organization mine = organization();
        Organization theirs = organization();
        User portalUser = user(mine);
        User unlinkedUser = user(mine);

        Vendor linked = vendor(mine, "VEN-2026-007", "Eta Components");
        linked.setUser(portalUser);
        vendorRepository.saveAndFlush(linked);
        vendorRepository.saveAndFlush(vendor(mine, "VEN-2026-008", "Theta Tools"));
        entityManager.flush();

        assertThat(vendorRepository.findIdsByUserIdAndOrganizationId(portalUser.getId(), mine.getId()))
                .containsExactly(linked.getId());
        assertThat(vendorRepository.findIdsByUserIdAndOrganizationId(portalUser.getId(), theirs.getId()))
                .isEmpty();
        assertThat(vendorRepository.findIdsByUserIdAndOrganizationId(
                unlinkedUser.getId(), mine.getId())).isEmpty();
    }

    @Test
    @Transactional
    void countsVendorsReferencingACategoryWithinTheOrganization() {
        Organization organization = organization();
        Organization other = organization();
        VendorCategory category = category(organization, "Logistics");

        Vendor first = vendor(organization, "VEN-2026-004", "Delta Freight");
        first.setCategory(category);
        Vendor second = vendor(organization, "VEN-2026-005", "Epsilon Freight");
        second.setCategory(category);
        vendorRepository.saveAll(List.of(first, second));
        entityManager.flush();

        assertThat(vendorRepository.countByOrganizationIdAndCategoryId(
                organization.getId(), category.getId())).isEqualTo(2L);
        assertThat(vendorRepository.countByOrganizationIdAndCategoryId(
                other.getId(), category.getId())).isZero();
    }

    @Test
    @Transactional
    void contactsAndDocumentsAreReachableOnlyThroughTheOwningOrganization() {
        Organization mine = organization();
        Organization theirs = organization();
        Vendor vendor = vendorRepository.saveAndFlush(
                vendor(mine, "VEN-2026-006", "Zeta Instruments"));

        VendorContact primary = contact(vendor, "Anita Rao", true);
        VendorContact abhay = contact(vendor, "Abhay Kumar", false);
        VendorContact zara = contact(vendor, "Zara Sheikh", false);
        vendorContactRepository.saveAll(List.of(zara, abhay, primary));

        VendorDocument document = new VendorDocument();
        document.setVendor(vendor);
        document.setDocumentType(VendorDocumentType.GST_CERTIFICATE);
        document.setFileName("gst.pdf");
        document.setFileUrl("attachment:" + UUID.randomUUID());
        document.setExpiryDate(LocalDate.now().plusDays(45));
        document.setUploadedAt(Instant.now());
        VendorDocument savedDocument = vendorDocumentRepository.saveAndFlush(document);
        entityManager.flush();
        entityManager.clear();

        assertThat(vendorContactRepository
                .findByVendorIdAndVendorOrganizationIdOrderByPrimaryContactDescNameAsc(
                        vendor.getId(), mine.getId()))
                .extracting(VendorContact::getName)
                .containsExactly("Anita Rao", "Abhay Kumar", "Zara Sheikh");
        assertThat(vendorContactRepository
                .findByVendorIdAndVendorOrganizationIdOrderByPrimaryContactDescNameAsc(
                        vendor.getId(), theirs.getId()))
                .isEmpty();
        assertThat(vendorContactRepository.findByIdAndVendorOrganizationId(
                primary.getId(), theirs.getId())).isEmpty();

        Optional<VendorDocument> reloaded = vendorDocumentRepository.findByIdAndVendorOrganizationId(
                savedDocument.getId(), mine.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getDocumentType()).isEqualTo(VendorDocumentType.GST_CERTIFICATE);
        assertThat(reloaded.get().getFileName()).isEqualTo("gst.pdf");
        assertThat(reloaded.get().getExpiryDate()).isEqualTo(LocalDate.now().plusDays(45));
        assertThat(reloaded.get().getUploadedAt()).isNotNull();
        assertThat(vendorDocumentRepository.findByIdAndVendorOrganizationId(
                savedDocument.getId(), theirs.getId())).isEmpty();
        assertThat(vendorDocumentRepository
                .findByVendorIdAndVendorOrganizationIdOrderByUploadedAtDesc(
                        vendor.getId(), theirs.getId()))
                .isEmpty();
    }

    // ----- fixtures -----

    private Organization organization() {
        Organization organization = new Organization();
        organization.setName("Vendor Test Org");
        organization.setSlug("vendor-" + UUID.randomUUID());
        return organizationRepository.saveAndFlush(organization);
    }

    private User user(Organization organization) {
        User user = new User();
        user.setOrganization(organization);
        user.setEmail("vendor-user-" + UUID.randomUUID() + "@example.test");
        user.setPasswordHash("irrelevant");
        user.setFirstName("Vendor");
        user.setLastName("Portal");
        return userRepository.saveAndFlush(user);
    }

    private VendorCategory category(Organization organization, String name) {
        VendorCategory category = new VendorCategory();
        category.setOrganization(organization);
        category.setName(name);
        category.setDescription(name + " suppliers");
        return vendorCategoryRepository.saveAndFlush(category);
    }

    private Vendor vendor(Organization organization, String code, String companyName) {
        Vendor vendor = new Vendor();
        vendor.setOrganization(organization);
        vendor.setVendorCode(code);
        vendor.setCompanyName(companyName);
        vendor.setEmail("contact-" + UUID.randomUUID() + "@example.test");
        vendor.setRegisteredAt(Instant.now());
        return vendor;
    }

    private VendorContact contact(Vendor vendor, String name, boolean primary) {
        VendorContact contact = new VendorContact();
        contact.setVendor(vendor);
        contact.setName(name);
        contact.setEmail(name.replace(' ', '.').toLowerCase() + "@example.test");
        contact.setDesignation("Manager");
        contact.setPrimaryContact(primary);
        return contact;
    }
}
