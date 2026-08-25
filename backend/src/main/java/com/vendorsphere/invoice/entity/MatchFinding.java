package com.vendorsphere.invoice.entity;

import com.vendorsphere.common.entity.CreatedOnlyEntity;
import com.vendorsphere.invoice.MatchFindingType;
import com.vendorsphere.invoice.MatchResolutionState;
import com.vendorsphere.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "invoice_match_findings")
public class MatchFinding extends CreatedOnlyEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_item_id")
    private com.vendorsphere.purchaseorder.entity.PurchaseOrderItem purchaseOrderItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "finding_type", nullable = false, length = 30)
    private MatchFindingType findingType;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "expected_value")
    private String expectedValue;

    @Column(name = "actual_value")
    private String actualValue;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_state", nullable = false, length = 20)
    private MatchResolutionState resolutionState = MatchResolutionState.UNRESOLVED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "overridden_by")
    private User overriddenBy;

    @Column(name = "overridden_at")
    private Instant overriddenAt;

    @Column(name = "override_justification", columnDefinition = "TEXT")
    private String overrideJustification;

    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }
    public com.vendorsphere.purchaseorder.entity.PurchaseOrderItem getPurchaseOrderItem() { return purchaseOrderItem; }
    public void setPurchaseOrderItem(com.vendorsphere.purchaseorder.entity.PurchaseOrderItem purchaseOrderItem) { this.purchaseOrderItem = purchaseOrderItem; }
    public MatchFindingType getFindingType() { return findingType; }
    public void setFindingType(MatchFindingType findingType) { this.findingType = findingType; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getExpectedValue() { return expectedValue; }
    public void setExpectedValue(String expectedValue) { this.expectedValue = expectedValue; }
    public String getActualValue() { return actualValue; }
    public void setActualValue(String actualValue) { this.actualValue = actualValue; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public MatchResolutionState getResolutionState() { return resolutionState; }
    public void setResolutionState(MatchResolutionState resolutionState) { this.resolutionState = resolutionState; }
    public User getOverriddenBy() { return overriddenBy; }
    public void setOverriddenBy(User overriddenBy) { this.overriddenBy = overriddenBy; }
    public Instant getOverriddenAt() { return overriddenAt; }
    public void setOverriddenAt(Instant overriddenAt) { this.overriddenAt = overriddenAt; }
    public String getOverrideJustification() { return overrideJustification; }
    public void setOverrideJustification(String overrideJustification) { this.overrideJustification = overrideJustification; }
}
