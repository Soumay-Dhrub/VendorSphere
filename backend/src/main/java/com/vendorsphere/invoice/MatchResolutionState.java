package com.vendorsphere.invoice;

/**
 * Resolution state of a recorded three-way matching finding. An invoice holding at least one
 * {@link #UNRESOLVED} finding cannot reach {@code APPROVED}, {@code PARTIALLY_PAID} or {@code PAID}.
 *
 * <p>Persisted as {@code VARCHAR} through {@code @Enumerated(EnumType.STRING)}.
 */
public enum MatchResolutionState {

    /** Open; blocks invoice approval and payment. */
    UNRESOLVED,

    /** Accepted by an authorised user with a recorded justification. */
    OVERRIDDEN
}
