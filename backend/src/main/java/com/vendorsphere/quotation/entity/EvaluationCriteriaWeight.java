package com.vendorsphere.quotation.entity;

import com.vendorsphere.common.entity.BaseEntity;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * The configurable evaluation weights of one organization (Requirement 16.10), one row per
 * organization by the V2 UNIQUE constraint. {@link com.vendorsphere.quotation.EvaluationEngine.Weights#DEFAULT}
 * applies while the row is absent (Requirement 16.9).
 */
@Entity
@Table(name = "evaluation_criteria_weights")
public class EvaluationCriteriaWeight extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    @Column(name = "price_weight", nullable = false, precision = 3, scale = 2)
    private BigDecimal priceWeight;

    @Column(name = "delivery_weight", nullable = false, precision = 3, scale = 2)
    private BigDecimal deliveryWeight;

    @Column(name = "performance_weight", nullable = false, precision = 3, scale = 2)
    private BigDecimal performanceWeight;

    @Column(name = "warranty_weight", nullable = false, precision = 3, scale = 2)
    private BigDecimal warrantyWeight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public BigDecimal getPriceWeight() {
        return priceWeight;
    }

    public void setPriceWeight(BigDecimal priceWeight) {
        this.priceWeight = priceWeight;
    }

    public BigDecimal getDeliveryWeight() {
        return deliveryWeight;
    }

    public void setDeliveryWeight(BigDecimal deliveryWeight) {
        this.deliveryWeight = deliveryWeight;
    }

    public BigDecimal getPerformanceWeight() {
        return performanceWeight;
    }

    public void setPerformanceWeight(BigDecimal performanceWeight) {
        this.performanceWeight = performanceWeight;
    }

    public BigDecimal getWarrantyWeight() {
        return warrantyWeight;
    }

    public void setWarrantyWeight(BigDecimal warrantyWeight) {
        this.warrantyWeight = warrantyWeight;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }
}
