package com.vendorsphere.vendor.entity;

import com.vendorsphere.common.entity.BaseEntity;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.vendor.VendorStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "vendors")
public class Vendor extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private VendorCategory category;

    @Column(name = "vendor_code", nullable = false, length = 50)
    private String vendorCode;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(nullable = false)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "tax_identifier", length = 100)
    private String taxIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VendorStatus status = VendorStatus.PROSPECTIVE;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO.setScale(2);

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Column(name = "status_change_reason", columnDefinition = "TEXT")
    private String statusChangeReason;

    @Version
    @Column(nullable = false)
    private long version;

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public VendorCategory getCategory() {
        return category;
    }

    public void setCategory(VendorCategory category) {
        this.category = category;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public void setVendorCode(String vendorCode) {
        this.vendorCode = vendorCode;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTaxIdentifier() {
        return taxIdentifier;
    }

    public void setTaxIdentifier(String taxIdentifier) {
        this.taxIdentifier = taxIdentifier;
    }

    public VendorStatus getStatus() {
        return status;
    }

    public void setStatus(VendorStatus status) {
        this.status = status;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    public String getStatusChangeReason() {
        return statusChangeReason;
    }

    public void setStatusChangeReason(String statusChangeReason) {
        this.statusChangeReason = statusChangeReason;
    }

    public long getVersion() {
        return version;
    }
}
