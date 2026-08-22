package com.vendorsphere.vendor;

import com.vendorsphere.common.util.StateMachine;
import java.util.EnumSet;
import java.util.Map;

/**
 * Permitted vendor status transitions, encoding acceptance criterion 3.1 exactly.
 *
 * <p>The listed pairs are PROSPECTIVE&rarr;ACTIVE, PROSPECTIVE&rarr;INACTIVE, ACTIVE&rarr;SUSPENDED,
 * ACTIVE&rarr;BLACKLISTED, ACTIVE&rarr;INACTIVE, SUSPENDED&rarr;ACTIVE, SUSPENDED&rarr;BLACKLISTED,
 * SUSPENDED&rarr;INACTIVE, BLACKLISTED&rarr;INACTIVE and INACTIVE&rarr;ACTIVE. No other pair is
 * permitted.
 */
public final class VendorStatusTransitions {

    /** Immutable machine over the acceptance criterion 3.1 transition table. */
    public static final StateMachine<VendorStatus> MACHINE =
            StateMachine.of(
                    Map.of(
                            VendorStatus.PROSPECTIVE,
                            EnumSet.of(VendorStatus.ACTIVE, VendorStatus.INACTIVE),
                            VendorStatus.ACTIVE,
                            EnumSet.of(
                                    VendorStatus.SUSPENDED,
                                    VendorStatus.BLACKLISTED,
                                    VendorStatus.INACTIVE),
                            VendorStatus.SUSPENDED,
                            EnumSet.of(
                                    VendorStatus.ACTIVE,
                                    VendorStatus.BLACKLISTED,
                                    VendorStatus.INACTIVE),
                            VendorStatus.BLACKLISTED,
                            EnumSet.of(VendorStatus.INACTIVE),
                            VendorStatus.INACTIVE,
                            EnumSet.of(VendorStatus.ACTIVE)));

    private VendorStatusTransitions() {}
}
