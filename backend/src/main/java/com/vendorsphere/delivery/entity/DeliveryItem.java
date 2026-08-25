package com.vendorsphere.delivery.entity;

import com.vendorsphere.common.entity.CreatedOnlyEntity;
import com.vendorsphere.purchaseorder.entity.PurchaseOrderItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "delivery_items")
public class DeliveryItem extends CreatedOnlyEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_item_id", nullable = false)
    private PurchaseOrderItem purchaseOrderItem;

    @Column(name = "received_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal receivedQuantity;

    @Column(name = "damaged_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal damagedQuantity = BigDecimal.ZERO.setScale(3);

    @Column(name = "rejected_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal rejectedQuantity = BigDecimal.ZERO.setScale(3);

    @Column(columnDefinition = "TEXT")
    private String notes;

    public Delivery getDelivery() { return delivery; }
    public void setDelivery(Delivery delivery) { this.delivery = delivery; }
    public PurchaseOrderItem getPurchaseOrderItem() { return purchaseOrderItem; }
    public void setPurchaseOrderItem(PurchaseOrderItem purchaseOrderItem) { this.purchaseOrderItem = purchaseOrderItem; }
    public BigDecimal getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(BigDecimal receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    public BigDecimal getDamagedQuantity() { return damagedQuantity; }
    public void setDamagedQuantity(BigDecimal damagedQuantity) { this.damagedQuantity = damagedQuantity; }
    public BigDecimal getRejectedQuantity() { return rejectedQuantity; }
    public void setRejectedQuantity(BigDecimal rejectedQuantity) { this.rejectedQuantity = rejectedQuantity; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
