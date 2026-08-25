package com.vendorsphere.rfq.entity;

import com.vendorsphere.common.entity.BaseEntity;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.procurement.entity.PurchaseRequest;
import com.vendorsphere.rfq.RfqStatus;
import com.vendorsphere.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * A Request for Quotation issued by one organization (Requirement 9.1).
 *
 * <p>{@code rfqs} carries {@code created_at} and {@code updated_at}, so this entity extends
 * {@link BaseEntity}; {@code version} maps the optimistic-lock column added by V2 (Requirement 32.3).
 *
 * <p>{@code purchaseRequest} is nullable in V1 but always set by {@code RfqService}, which creates
 * every RFQ from a source request and retains the link for the derived-RFQ read of Requirement 8.9.
 * Items and invitations are deliberately not mapped as collections - they are read through their own
 * repositories batched per page, as everywhere else on the platform.
 */
@Entity
@Table(name = "rfqs")
public class Rfq extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_request_id")
    private PurchaseRequest purchaseRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "rfq_number", nullable = false, length = 50)
    private String rfqNumber;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "opening_date", nullable = false)
    private Instant openingDate;

    @Column(name = "closing_date", nullable = false)
    private Instant closingDate;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "delivery_location", columnDefinition = "TEXT")
    private String deliveryLocation;

    @Column(columnDefinition = "TEXT")
    private String terms;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RfqStatus status = RfqStatus.DRAFT;

    /** Why the RFQ was cancelled; set only on cancellation (Requirement 11.7). */
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Version
    @Column(nullable = false)
    private long version;

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public PurchaseRequest getPurchaseRequest() {
        return purchaseRequest;
    }

    public void setPurchaseRequest(PurchaseRequest purchaseRequest) {
        this.purchaseRequest = purchaseRequest;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public String getRfqNumber() {
        return rfqNumber;
    }

    public void setRfqNumber(String rfqNumber) {
        this.rfqNumber = rfqNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getOpeningDate() {
        return openingDate;
    }

    public void setOpeningDate(Instant openingDate) {
        this.openingDate = openingDate;
    }

    public Instant getClosingDate() {
        return closingDate;
    }

    public void setClosingDate(Instant closingDate) {
        this.closingDate = closingDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDeliveryLocation() {
        return deliveryLocation;
    }

    public void setDeliveryLocation(String deliveryLocation) {
        this.deliveryLocation = deliveryLocation;
    }

    public String getTerms() {
        return terms;
    }

    public void setTerms(String terms) {
        this.terms = terms;
    }

    public RfqStatus getStatus() {
        return status;
    }

    public void setStatus(RfqStatus status) {
        this.status = status;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public long getVersion() {
        return version;
    }
}
