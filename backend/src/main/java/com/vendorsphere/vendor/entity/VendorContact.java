package com.vendorsphere.vendor.entity;

import com.vendorsphere.common.entity.CreatedOnlyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One named contact of a vendor (Requirement 4.1).
 *
 * <p>{@code vendor_contacts} carries {@code created_at} but no {@code updated_at}, so this entity
 * extends {@link CreatedOnlyEntity}. The row reaches its organization through its vendor, which is
 * why the repository finders traverse {@code vendor.organization.id} rather than a local column.
 */
@Entity
@Table(name = "vendor_contacts")
public class VendorContact extends CreatedOnlyEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(nullable = false)
    private String name;

    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 100)
    private String designation;

    /** At most one contact per vendor holds this flag; the service layer enforces that. */
    @Column(name = "primary_contact", nullable = false)
    private boolean primaryContact = false;

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public boolean isPrimaryContact() {
        return primaryContact;
    }

    public void setPrimaryContact(boolean primaryContact) {
        this.primaryContact = primaryContact;
    }
}
