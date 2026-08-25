package com.vendorsphere.analytics.entity;

import com.vendorsphere.common.entity.IdentifiedEntity;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.vendor.entity.Vendor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One month's scored performance of a vendor (Requirement 26.9), upserted per period. */
@Entity
@Table(name = "vendor_performance_snapshots")
public class VendorPerformanceSnapshot extends IdentifiedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "delivery_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal deliveryScore = BigDecimal.ZERO.setScale(2);

    @Column(name = "quality_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal qualityScore = BigDecimal.ZERO.setScale(2);

    @Column(name = "pricing_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal pricingScore = BigDecimal.ZERO.setScale(2);

    @Column(name = "responsiveness_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal responsivenessScore = BigDecimal.ZERO.setScale(2);

    @Column(name = "fulfilment_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal fulfilmentScore = BigDecimal.ZERO.setScale(2);

    @Column(name = "overall_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal overallScore = BigDecimal.ZERO.setScale(2);

    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }
    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public BigDecimal getDeliveryScore() { return deliveryScore; }
    public void setDeliveryScore(BigDecimal deliveryScore) { this.deliveryScore = deliveryScore; }
    public BigDecimal getQualityScore() { return qualityScore; }
    public void setQualityScore(BigDecimal qualityScore) { this.qualityScore = qualityScore; }
    public BigDecimal getPricingScore() { return pricingScore; }
    public void setPricingScore(BigDecimal pricingScore) { this.pricingScore = pricingScore; }
    public BigDecimal getResponsivenessScore() { return responsivenessScore; }
    public void setResponsivenessScore(BigDecimal responsivenessScore) { this.responsivenessScore = responsivenessScore; }
    public BigDecimal getFulfilmentScore() { return fulfilmentScore; }
    public void setFulfilmentScore(BigDecimal fulfilmentScore) { this.fulfilmentScore = fulfilmentScore; }
    public BigDecimal getOverallScore() { return overallScore; }
    public void setOverallScore(BigDecimal overallScore) { this.overallScore = overallScore; }
}
