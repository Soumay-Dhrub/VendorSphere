package com.vendorsphere.vendor;

import com.vendorsphere.vendor.service.VendorDocumentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * The daily vendor document expiry evaluation (Requirement 5.6).
 *
 * <p>Runs at 00:30 UTC once per calendar day, as the schedule table pins, and delegates the whole
 * evaluation to {@link VendorDocumentService#notifyDocumentsExpiringOn} so the rule and its tests
 * live in one place; this class owns only the clock read and the trigger. The zone is stated
 * explicitly because a server in any other offset must not shift which calendar day the run belongs
 * to.
 */
@Component
public class VendorDocumentExpiryJob {

    private final VendorDocumentService vendorDocumentService;
    private final Clock clock;

    public VendorDocumentExpiryJob(VendorDocumentService vendorDocumentService, Clock clock) {
        this.vendorDocumentService = vendorDocumentService;
        this.clock = clock;
    }

    @Scheduled(cron = "0 30 0 * * *", zone = "UTC")
    public void run() {
        vendorDocumentService.notifyDocumentsExpiringOn(LocalDate.now(clock));
    }
}
