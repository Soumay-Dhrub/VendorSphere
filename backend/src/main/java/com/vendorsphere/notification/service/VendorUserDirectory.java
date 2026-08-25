package com.vendorsphere.notification.service;

import java.util.List;
import java.util.UUID;

public interface VendorUserDirectory {

    List<UUID> findUserIdsOfVendor(UUID vendorId);
}
