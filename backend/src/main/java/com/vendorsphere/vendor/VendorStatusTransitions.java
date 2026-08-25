package com.vendorsphere.vendor;

import com.vendorsphere.common.util.StateMachine;
import java.util.EnumSet;
import java.util.Map;

public final class VendorStatusTransitions {

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
