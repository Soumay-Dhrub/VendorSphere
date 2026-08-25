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

@Service
public class VendorCategoryService {

    static final String DUPLICATE_NAME_MESSAGE = "Vendor category already exists";

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

    @Transactional
    public VendorCategoryResponse create(VendorCategoryRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        assertNameAvailable(organizationId, request.name());

        VendorCategory category = new VendorCategory();
        category.setOrganization(organizationRepository.getReferenceById(organizationId));
        applyFields(category, request);

        return VendorCategoryResponse.from(save(category));
    }

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

    @Transactional(readOnly = true)
    public List<VendorCategoryResponse> list() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return vendorCategoryRepository.findByOrganizationIdOrderByNameAsc(organizationId).stream()
                .map(VendorCategoryResponse::from)
                .toList();
    }

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
