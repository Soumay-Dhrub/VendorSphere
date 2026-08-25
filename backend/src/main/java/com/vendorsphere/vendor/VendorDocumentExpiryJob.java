package com.vendorsphere.vendor;

import com.vendorsphere.vendor.service.VendorDocumentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

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
