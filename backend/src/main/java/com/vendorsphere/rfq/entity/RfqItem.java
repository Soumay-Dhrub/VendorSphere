package com.vendorsphere.rfq.entity;

import com.vendorsphere.common.entity.CreatedOnlyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One line of an RFQ, copied from a purchase request item at creation (Requirement 9.3).
 *
 * <p>{@code rfq_items} carries {@code created_at} only, so this entity extends
 * {@link CreatedOnlyEntity}. {@code sourceItemId} maps {@code purchase_request_item_id}, retaining
 * the link back to the line the requirement asked for; it is a plain column because the item is
 * never navigated as an association, and the PR module owns that table.
 */
@Entity
@Table(name = "rfq_items")
public class RfqItem extends CreatedOnlyEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_id", nullable = false)
    private Rfq rfq;

    @Column(name = "purchase_request_item_id")
    private UUID sourceItemId;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, length = 50)
    private String unit = "UNIT";

    @Column(columnDefinition = "TEXT")
    private String specification;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public Rfq getRfq() {
        return rfq;
    }

    public void setRfq(Rfq rfq) {
        this.rfq = rfq;
    }

    public UUID getSourceItemId() {
        return sourceItemId;
    }

    public void setSourceItemId(UUID sourceItemId) {
        this.sourceItemId = sourceItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
