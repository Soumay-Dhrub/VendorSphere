package com.vendorsphere.quotation.entity;

import com.vendorsphere.common.entity.IdentifiedEntity;
import com.vendorsphere.rfq.entity.Rfq;
import com.vendorsphere.quotation.entity.Quotation;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.vendor.entity.Vendor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "vendor_selections")
public class VendorSelection extends IdentifiedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_id", nullable = false, unique = true)
    private Rfq rfq;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quotation_id", nullable = false)
    private Quotation quotation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "selected_by", nullable = false)
    private User selectedBy;

    @Column(name = "selection_justification", columnDefinition = "TEXT")
    private String justification;

    @Column(name = "selected_at", nullable = false)
    private Instant selectedAt;

    public Rfq getRfq() {
        return rfq;
    }

    public void setRfq(Rfq rfq) {
        this.rfq = rfq;
    }

    public Quotation getQuotation() {
        return quotation;
    }

    public void setQuotation(Quotation quotation) {
        this.quotation = quotation;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public User getSelectedBy() {
        return selectedBy;
    }

    public void setSelectedBy(User selectedBy) {
        this.selectedBy = selectedBy;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public Instant getSelectedAt() {
        return selectedAt;
    }

    public void setSelectedAt(Instant selectedAt) {
        this.selectedAt = selectedAt;
    }
}
