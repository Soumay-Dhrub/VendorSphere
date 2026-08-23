package com.vendorsphere.vendor.service;

import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.organization.repository.OrganizationRepository;
import com.vendorsphere.vendor.dto.VendorCategoryRequest;
import com.vendorsphere.vendor.dto.VendorCategoryResponse;
import com.vendorsphere.vendor.entity.VendorCategory;
import com.vendorsphere.vendor.repository.VendorCategoryRepository;
import com.vendorsphere.vendor.repository.VendorRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * The vendor category taxonomy of one organization (Requirements 4.4, 4.5, 4.6).
 *
 * <p>Every read is keyed on the caller's organization, so a category identifier belonging to another
 * tenant misses and surfaces as 404 {@code Vendor category not found} rather than 403
 * (Requirement 30.10). That is the same wording {@code VendorService} reports when a vendor profile
 * references an unreachable category, so a client sees one message for one condition.
 *
 * <h4>Why uniqueness is checked twice</h4>
 *
 * <p>Requirement 4.5 pins 409 {@code Vendor category already exists} for a name another category of
 * the same organization already holds. Two mechanisms can catch that, and both are used:
 *
 * <ul>
 *   <li>the case-insensitive {@code existsByOrganizationIdAndNameIgnoreCase} check, which produces the
 *       pinned message on the ordinary path and treats {@code Hardware} and {@code hardware} as the
 *       same name — the same case-insensitive reading {@code VendorService} applies to the vendor email
 *       uniqueness of Requirement 2.3;</li>
 *   <li>the {@code UNIQUE (organization_id, name)} constraint from V1, which closes the read-then-write
 *       race two concurrent creations would otherwise slip through. The insert is flushed inside this
 *       method so the violation can be translated into the same pinned 409 instead of reaching the
 *       generic handler as a 500.</li>
 * </ul>
 *
 * <p>The two do not cover exactly the same set: the check is case-insensitive while the database
 * constraint is not, so two concurrent creations of {@code Hardware} and {@code hardware} can both
 * commit — each passes its own check before the other is visible, and the constraint does not consider
 * them equal. Closing that would need a case-insensitive unique index, which belongs to a migration
 * rather than to this service, so the residual window is documented rather than papered over.
 */
@Service
public class VendorCategoryService {

    /** Pinned by Requirement 4.5, reported by both the pre-check and the constraint translation. */
    static final String DUPLICATE_NAME_MESSAGE = "Vendor category already exists";

    /** Matches the wording {@code VendorService} uses for an unreachable category. */
    static final String NOT_FOUND_MESSAGE = "Vendor category not found";

    private final VendorCategoryRepository vendorCategoryRepository;
    private final VendorRepository vendorRepository;
    private final OrganizationRepository organizationRepository;

    public VendorCategoryService(
            VendorCategoryRepository vendorCategoryRepository,
            VendorRepository vendorRepository,
            OrganizationRepository organizationRepository
    ) {
        this.vendorCategoryRepository = vendorCategoryRepository;
        this.vendorRepository = vendorRepository;
        this.organizationRepository = organizationRepository;
    }

    /**
     * Creates a category with the supplied name and description for the caller's organization
     * (Requirement 4.4), rejecting a name already held in that organization with 409
     * {@code Vendor category already exists} (Requirement 4.5).
     */
    @Transactional
    public VendorCategoryResponse create(VendorCategoryRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        assertNameAvailable(organizationId, request.name());

        VendorCategory category = new VendorCategory();
        category.setOrganization(organizationRepository.getReferenceById(organizationId));
        applyFields(category, request);

        return VendorCategoryResponse.from(save(category));
    }

    /**
     * Renames or re-describes an existing category of the caller's organization.
     *
     * <p>The uniqueness guard runs only when the name actually changes, so re-submitting a category's
     * own name — in any case — is not a clash with itself, while moving onto a name another category
     * of the same organization holds is rejected with the 409 of Requirement 4.5. Requirement 4.5 is
     * written about creation, but per-organization uniqueness would be pointless if an update could
     * break it, and without the guard the database constraint would surface as a 500.
     */
    @Transactional
    public VendorCategoryResponse update(UUID categoryId, VendorCategoryRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        VendorCategory category = findInOrganization(categoryId, organizationId);

        if (!category.getName().equalsIgnoreCase(request.name())) {
            assertNameAvailable(organizationId, request.name());
        }
        applyFields(category, request);

        return VendorCategoryResponse.from(save(category));
    }

    /** The organization's categories by name ascending. */
    @Transactional(readOnly = true)
    public List<VendorCategoryResponse> list() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return vendorCategoryRepository.findByOrganizationIdOrderByNameAsc(organizationId).stream()
                .map(VendorCategoryResponse::from)
                .toList();
    }

    /**
     * Deletes a category that no vendor references (Requirement 4.6).
     *
     * <p>A category still in use is refused with 409 and a message stating how many vendors reference
     * it, so the caller learns the size of the reassignment ahead of them rather than only that the
     * delete failed. The count is read inside the same transaction as the delete, and
     * {@code vendors.category_id} is a plain nullable foreign key with no cascade, so deleting an
     * unreferenced category cannot orphan a vendor.
     */
    @Transactional
    public void delete(UUID categoryId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        VendorCategory category = findInOrganization(categoryId, organizationId);

        long referencingVendors =
                vendorRepository.countByOrganizationIdAndCategoryId(organizationId, categoryId);
        if (referencingVendors > 0) {
            throw new BusinessException(inUseMessage(referencingVendors), HttpStatus.CONFLICT);
        }
        vendorCategoryRepository.delete(category);
    }

    /**
     * The 409 wording of Requirement 4.6, which asks for the number of vendors referencing the
     * category. Singular and plural are distinguished so a message about one vendor reads correctly.
     */
    static String inUseMessage(long referencingVendors) {
        return "Vendor category is referenced by " + referencingVendors
                + (referencingVendors == 1 ? " vendor" : " vendors");
    }

    private VendorCategory findInOrganization(UUID categoryId, UUID organizationId) {
        return vendorCategoryRepository.findByIdAndOrganizationId(categoryId, organizationId)
                .orElseThrow(() -> new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
    }

    private void assertNameAvailable(UUID organizationId, String name) {
        if (vendorCategoryRepository.existsByOrganizationIdAndNameIgnoreCase(organizationId, name)) {
            throw new BusinessException(DUPLICATE_NAME_MESSAGE, HttpStatus.CONFLICT);
        }
    }

    /**
     * Persists the category and flushes, so a {@code UNIQUE (organization_id, name)} violation raised
     * by a concurrent write is reported as the pinned 409 rather than escaping this service as an
     * unexpected 500. The surrounding transaction is already rollback-only by then, and the thrown
     * {@link BusinessException} rolls it back, so nothing is left half-written.
     */
    private VendorCategory save(VendorCategory category) {
        try {
            return vendorCategoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(DUPLICATE_NAME_MESSAGE, HttpStatus.CONFLICT);
        }
    }

    private static void applyFields(VendorCategory category, VendorCategoryRequest request) {
        category.setName(request.name());
        category.setDescription(request.description());
    }
}
