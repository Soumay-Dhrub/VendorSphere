package com.vendorsphere.procurement.entity;

import com.vendorsphere.common.entity.CreatedOnlyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * One line of a purchase request (Requirement 7.4).
 *
 * <p>{@code purchase_request_items} carries {@code created_at} but no {@code updated_at}, so this
 * entity extends {@link CreatedOnlyEntity}. {@code quantity} maps the {@code DECIMAL(12, 3)} column;
 * every stored value is normalized to {@link com.vendorsphere.common.util.Money#QUANTITY_SCALE} by
 * the service, so no arithmetic or rescaling happens here.
 *
 * <p>{@code sortOrder} is assigned by the service as the current item count at creation and is never
 * reordered afterwards, so items keep authoring order on every read (Requirement 7.4).
 */
@Entity
@Table(name = "purchase_request_items")
public class PurchaseRequestItem extends CreatedOnlyEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_request_id", nullable = false)
    private PurchaseRequest purchaseRequest;

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

    public PurchaseRequest getPurchaseRequest() {
        return purchaseRequest;
    }

    public void setPurchaseRequest(PurchaseRequest purchaseRequest) {
        this.purchaseRequest = purchaseRequest;
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
