package com.vendorsphere.vendor.service;

import java.util.Optional;
import java.util.UUID;

public interface VendorAccessGuard {

    Optional<UUID> currentVendorId();

    void assertVendorVisible(UUID recordVendorId, String notFoundMessage);
}
