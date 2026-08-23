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

/**
 * The named contacts of a vendor (Requirements 4.1, 4.2, 4.3).
 *
 * <p>Contacts are reached through their vendor, so every operation loads that vendor keyed on the
 * caller's organization first. A vendor identifier belonging to another tenant misses and surfaces as
 * 404 {@code Vendor not found} — the message {@link VendorService} pins for Requirement 2.6 — rather
 * than 403, which keeps identifiers unenumerable (Requirements 2.6, 30.10). A contact identifier that
 * exists but hangs off a different vendor is reported as {@code Vendor contact not found} for the same
 * reason: the nested path {@code /vendors/{id}/contacts/{contactId}} is only satisfied when both parts
 * agree.
 *
 * <h4>Single primary contact</h4>
 *
 * <p>Requirement 4.2 fires only when a contact is written <em>with the flag set to true</em>, and its
 * effect is to clear the flag on the vendor's other contacts. That is an at-most-one rule, not an
 * exactly-one rule: a vendor whose contacts were all created with the flag absent legitimately has no
 * primary contact, and removing the primary contact does not promote a replacement. Nothing here
 * invents a primary the caller did not ask for.
 *
 * <p>The demotion and the promotion share one transaction, so no reader can observe two primary
 * contacts on the same vendor (Requirement 32.1).
 */
@Service
public class VendorContactService {

    /**
     * Not pinned by Requirement 4, but kept as a constant because three operations report it and the
     * wording follows the {@code <Entity> not found} convention the design fixes for tenant-scoped
     * misses.
     */
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

    /**
     * Adds a contact carrying name, email, phone and designation to the vendor (Requirement 4.1),
     * demoting the vendor's other contacts when this one arrives as primary (Requirement 4.2).
     */
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

    /**
     * Applies the supplied fields to an existing contact of the vendor, demoting the vendor's other
     * contacts when this one is written as primary (Requirement 4.2).
     *
     * <p>A request that sets the flag to false simply clears it on this contact and touches nothing
     * else: Requirement 4.2 only describes the true case, and demoting the sole primary is a
     * deliberate act by an authorised caller.
     */
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

    /** Removes one contact of the vendor. */
    @Transactional
    public void delete(UUID vendorId, UUID contactId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        vendorContactRepository.delete(findContact(vendorId, contactId, organizationId));
    }

    /**
     * Returns the vendor's contacts with the primary contact first and the rest by name ascending
     * (Requirement 4.3).
     *
     * <p>The ordering is the repository finder's, not a sort applied here, so it is enforced by the
     * database rather than restated in Java. The vendor is resolved first so an unknown or
     * cross-tenant identifier answers 404 rather than an empty list, which would otherwise leak that
     * the vendor exists elsewhere.
     */
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

    /**
     * Resolves a contact of one vendor of the caller's organization. Both halves of the nested path
     * are checked: the vendor must exist for the tenant, and the contact must belong to that vendor.
     */
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

    /**
     * Clears the primary flag on every contact of the vendor except {@code keepId}
     * (Requirement 4.2).
     *
     * <p>Reads the vendor's contacts through the tenant-scoped finder and writes back only the rows
     * that actually held the flag, so demoting on a vendor that had no previous primary issues no
     * update at all. Running as a loop over managed entities rather than a bulk {@code UPDATE} keeps
     * the persistence context consistent with the database within the transaction, which matters
     * because the caller returns the saved contact straight afterwards.
     */
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
