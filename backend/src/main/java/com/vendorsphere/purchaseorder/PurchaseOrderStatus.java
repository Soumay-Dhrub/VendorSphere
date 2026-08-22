package com.vendorsphere.purchaseorder;

/**
 * Lifecycle status of a purchase order.
 *
 * <p>Persisted as {@code VARCHAR} through {@code @Enumerated(EnumType.STRING)}, matching the
 * {@code chk_po_status} constraint on {@code purchase_orders.status}. Permitted transitions are held
 * by {@link PurchaseOrderStatusTransitions}.
 */
public enum PurchaseOrderStatus {

    /** Generated from an award but not yet sent to the vendor. */
    DRAFT,

    /** Issued to the vendor. */
    ISSUED,

    /** Acknowledged by the vendor. */
    ACKNOWLEDGED,

    /** At least one item is outstanding after one or more deliveries. */
    PARTIALLY_DELIVERED,

    /** Every ordered quantity has been received. */
    DELIVERED,

    /** Commercially complete. */
    CLOSED,

    /** Abandoned with a recorded reason. */
    CANCELLED
}
