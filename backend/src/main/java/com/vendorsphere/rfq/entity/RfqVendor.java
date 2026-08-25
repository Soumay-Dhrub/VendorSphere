package com.vendorsphere.rfq.entity;

import com.vendorsphere.common.entity.IdentifiedEntity;
import com.vendorsphere.rfq.RfqVendorStatus;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.vendor.entity.Vendor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One vendor's invitation to one RFQ (Requirement 10.1).
 *
 * <p>{@code rfq_vendors} carries no timestamp column of its own - {@code invited_at} plays that role
 * - so this entity extends {@link IdentifiedEntity}. The V1 unique constraint on
 * {@code (rfq_id, vendor_id)} backs the already-invited guard of Requirement 10.3 at the database
 * even though the service checks first.
 */
@Entity
@Table(name = "rfq_vendors")
public class RfqVendor extends IdentifiedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_id", nullable = false)
    private Rfq rfq;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "invited_at", nullable = false)
    private Instant invitedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RfqVendorStatus status = RfqVendorStatus.INVITED;

    public Rfq getRfq() {
        return rfq;
    }

    public void setRfq(Rfq rfq) {
        this.rfq = rfq;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public Instant getInvitedAt() {
        return invitedAt;
    }

    public void setInvitedAt(Instant invitedAt) {
        this.invitedAt = invitedAt;
    }

    public User getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(User invitedBy) {
        this.invitedBy = invitedBy;
    }

    public RfqVendorStatus getStatus() {
        return status;
    }

    public void setStatus(RfqVendorStatus status) {
        this.status = status;
    }
}
