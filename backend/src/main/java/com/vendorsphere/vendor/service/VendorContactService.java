package com.vendorsphere.vendor.service;

import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.vendor.dto.VendorContactRequest;
import com.vendorsphere.vendor.dto.VendorContactResponse;
import com.vendorsphere.vendor.entity.Vendor;
import com.vendorsphere.vendor.entity.VendorContact;
import com.vendorsphere.vendor.repository.VendorContactRepository;
import com.vendorsphere.vendor.repository.VendorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class VendorContactService {

    static final String NOT_FOUND_MESSAGE = "Vendor contact not found";

    private final VendorContactRepository vendorContactRepository;
    private final VendorRepository vendorRepository;

    public VendorContactService(
            VendorContactRepository vendorContactRepository,
            VendorRepository vendorRepository
    ) {
        this.vendorContactRepository = vendorContactRepository;
        this.vendorRepository = vendorRepository;
    }

    @Transactional
    public VendorContactResponse add(UUID vendorId, VendorContactRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Vendor vendor = findVendor(vendorId, organizationId);

        VendorContact contact = new VendorContact();
        contact.setVendor(vendor);
        applyFields(contact, request);

        VendorContact saved = vendorContactRepository.save(contact);
        if (saved.isPrimaryContact()) {
            demoteOtherContacts(vendorId, organizationId, saved.getId());
        }
        return VendorContactResponse.from(saved);
    }

    @Transactional
    public VendorContactResponse update(
            UUID vendorId, UUID contactId, VendorContactRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        VendorContact contact = findContact(vendorId, contactId, organizationId);

        applyFields(contact, request);

        VendorContact saved = vendorContactRepository.save(contact);
        if (saved.isPrimaryContact()) {
            demoteOtherContacts(vendorId, organizationId, saved.getId());
        }
        return VendorContactResponse.from(saved);
    }

    @Transactional
    public void delete(UUID vendorId, UUID contactId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        vendorContactRepository.delete(findContact(vendorId, contactId, organizationId));
    }

    @Transactional(readOnly = true)
    public List<VendorContactResponse> list(UUID vendorId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        findVendor(vendorId, organizationId);
        return vendorContactRepository
                .findByVendorIdAndVendorOrganizationIdOrderByPrimaryContactDescNameAsc(
                        vendorId, organizationId)
                .stream()
                .map(VendorContactResponse::from)
                .toList();
    }

    private Vendor findVendor(UUID vendorId, UUID organizationId) {
        return vendorRepository.findByIdAndOrganizationId(vendorId, organizationId)
                .orElseThrow(() -> new BusinessException(
                        VendorService.NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
    }

    private VendorContact findContact(UUID vendorId, UUID contactId, UUID organizationId) {
        findVendor(vendorId, organizationId);
        VendorContact contact = vendorContactRepository
                .findByIdAndVendorOrganizationId(contactId, organizationId)
                .orElseThrow(() -> new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        if (!contact.getVendor().getId().equals(vendorId)) {
            throw new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND);
        }
        return contact;
    }

    private static void applyFields(VendorContact contact, VendorContactRequest request) {
        contact.setName(request.name());
        contact.setEmail(request.email());
        contact.setPhone(request.phone());
        contact.setDesignation(request.designation());
        contact.setPrimaryContact(request.primaryContact());
    }

    private void demoteOtherContacts(UUID vendorId, UUID organizationId, UUID keepId) {
        for (VendorContact other : vendorContactRepository
                .findByVendorIdAndVendorOrganizationIdOrderByPrimaryContactDescNameAsc(
                        vendorId, organizationId)) {
            if (!other.getId().equals(keepId) && other.isPrimaryContact()) {
                other.setPrimaryContact(false);
                vendorContactRepository.save(other);
            }
        }
    }
}
