package com.vendorsphere.analytics.repository;

import com.vendorsphere.analytics.entity.VendorPerformanceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Snapshot reads; UNIQUE (vendor_id, period_start, period_end) backs the upsert. */
public interface VendorPerformanceSnapshotRepository
        extends JpaRepository<VendorPerformanceSnapshot, UUID> {

    Optional<VendorPerformanceSnapshot> findByVendorIdAndPeriodStartAndPeriodEnd(
            UUID vendorId, LocalDate periodStart, LocalDate periodEnd);
}
