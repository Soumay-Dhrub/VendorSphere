package com.vendorsphere.procurement;

/**
 * Urgency of a purchase request (Requirement 7.2). {@link #MEDIUM} is the default applied when a
 * request omits a priority.
 *
 * <p>Persisted as {@code VARCHAR} through {@code @Enumerated(EnumType.STRING)}, matching the
 * {@code chk_pr_priority} constraint on {@code purchase_requests.priority}.
 */
public enum Priority {

    /** Can wait beyond the normal procurement cycle. */
    LOW,

    /** Normal procurement cycle. Default when unspecified. */
    MEDIUM,

    /** Should be expedited ahead of normal requests. */
    HIGH,

    /** Requires immediate attention. */
    URGENT
}
