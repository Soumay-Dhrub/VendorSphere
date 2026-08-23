package com.vendorsphere.notification;

/**
 * The sixteen notifiable events of Requirement 28.2, in the order the requirement lists them.
 *
 * <p>The name is persisted in {@code notifications.event_type} and is one of the four columns of the
 * dedupe index {@code uq_notifications_event}, so a value here is part of the idempotence key of
 * Requirement 28.9. Renaming a constant changes that key and would let an already-sent notification
 * be sent again; add a constant rather than rename one.
 */
public enum NotificationEvent {

    PURCHASE_REQUEST_SUBMITTED,
    PURCHASE_REQUEST_APPROVED,
    PURCHASE_REQUEST_REJECTED,
    VENDOR_INVITED_TO_RFQ,
    RFQ_CLOSING_WITHIN_24_HOURS,
    QUOTATION_SUBMITTED,
    VENDOR_SELECTED,
    PURCHASE_ORDER_ISSUED,
    DELIVERY_RECORDED,
    INVOICE_SUBMITTED,
    INVOICE_APPROVED,
    INVOICE_REJECTED,
    PAYMENT_RECORDED,
    INVOICE_MATCH_EXCEPTION_RAISED,
    OVERDUE_DELIVERY_DETECTED,
    VENDOR_DOCUMENT_EXPIRING
}
