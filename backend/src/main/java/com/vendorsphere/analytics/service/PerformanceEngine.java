package com.vendorsphere.analytics.service;

import com.vendorsphere.analytics.engine.PerformanceCalculator;
import com.vendorsphere.analytics.entity.VendorPerformanceSnapshot;
import com.vendorsphere.analytics.repository.AnalyticsQueryRepository;
import com.vendorsphere.analytics.repository.VendorPerformanceSnapshotRepository;
import com.vendorsphere.common.util.Money;
import com.vendorsphere.organization.repository.OrganizationRepository;
import com.vendorsphere.vendor.entity.Vendor;
import com.vendorsphere.vendor.repository.VendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Vendor performance recalculation (Requirement 26.9): aggregates the raw counts, scores them with
 * {@link PerformanceCalculator}, upserts the current calendar month's snapshot and updates the
 * vendor's rating to score/20 (Requirement 26.11).
 *
 * <p>Recalculation is synchronous in the caller's transaction - the design accepts this at MVP scale
 * because the aggregates are single-vendor reads and no message broker is in scope.
 */
@Service
public class PerformanceEngine {

    private final AnalyticsQueryRepository analyticsQueries;
    private final VendorPerformanceSnapshotRepository snapshotRepository;
    private final VendorRepository vendorRepository;
    private final OrganizationRepository organizationRepository;
    private final Clock clock;

    public PerformanceEngine(
            AnalyticsQueryRepository analyticsQueries,
            VendorPerformanceSnapshotRepository snapshotRepository,
            VendorRepository vendorRepository,
            OrganizationRepository organizationRepository,
            Clock clock
    ) {
        this.analyticsQueries = analyticsQueries;
        this.snapshotRepository = snapshotRepository;
        this.vendorRepository = vendorRepository;
        this.organizationRepository = organizationRepository;
        this.clock = clock;
    }

    /**
     * Recalculates one vendor's metrics for the current calendar month. Safe to call on every
     * triggering event; the snapshot upsert keeps repeated calls within a month idempotent.
     */
    @Transactional
    public void recalculate(UUID vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId).orElse(null);
        if (vendor == null) {
            return;
        }

        List<Object[]> deliveryRows = analyticsQueries.deliveryCounts(vendorId);
        long deliveriesTotal = ((Number) deliveryRows.get(0)[0]).longValue();
        long deliveriesOnTime = ((Number) deliveryRows.get(0)[1]).longValue();

        List<Object[]> qualityRows = analyticsQueries.qualityQuantities(vendorId);
        BigDecimal received = asDecimal(qualityRows.get(0)[0]);
        BigDecimal rejected = asDecimal(qualityRows.get(0)[1]);

        BigDecimal pricingMean = analyticsQueries.pricingRatioMean(vendorId).stream()
                .findFirst().orElse(null);

        List<Object[]> responsivenessRows = analyticsQueries.responsivenessCounts(vendorId);
        long invitations = ((Number) responsivenessRows.get(0)[0]).longValue();
        long quotedInTime = ((Number) responsivenessRows.get(0)[1]).longValue();

        List<Object[]> fulfilmentRows = analyticsQueries.fulfilmentCounts(vendorId);
        long ordersCounted = ((Number) fulfilmentRows.get(0)[0]).longValue();
        long ordersFulfilled = ((Number) fulfilmentRows.get(0)[1]).longValue();

        PerformanceCalculator.Scores scores = PerformanceCalculator.compute(
                new PerformanceCalculator.Inputs(
                        deliveriesTotal, deliveriesOnTime, received, rejected, pricingMean,
                        invitations, quotedInTime, ordersCounted, ordersFulfilled));

        LocalDate today = LocalDate.now(clock);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        VendorPerformanceSnapshot snapshot = snapshotRepository
                .findByVendorIdAndPeriodStartAndPeriodEnd(vendorId, monthStart, monthEnd)
                .orElseGet(() -> {
                    VendorPerformanceSnapshot created = new VendorPerformanceSnapshot();
                    created.setVendor(vendor);
                    created.setOrganization(vendor.getOrganization() != null
                            ? vendor.getOrganization()
                            : organizationRepository.getReferenceById(
                                    java.util.UUID.randomUUID()));
                    created.setPeriodStart(monthStart);
                    created.setPeriodEnd(monthEnd);
                    return created;
                });
        snapshot.setDeliveryScore(scores.delivery());
        snapshot.setQualityScore(scores.quality());
        snapshot.setPricingScore(scores.pricing());
        snapshot.setResponsivenessScore(scores.responsiveness());
        snapshot.setFulfilmentScore(scores.fulfilment());
        snapshot.setOverallScore(scores.overall());
        snapshotRepository.save(snapshot);

        // Requirement 26.11: the rating follows the score.
        vendor.setRating(PerformanceCalculator.vendorRating(scores.overall()));
        vendorRepository.save(vendor);
    }

    /** The vendor's current overall score: latest snapshot, or the rating-derived figure. */
    @Transactional(readOnly = true)
    public BigDecimal currentScore(UUID vendorId) {
        return vendorRepository.findLatestPerformanceScore(vendorId)
                .orElseGet(() -> vendorRepository.findById(vendorId)
                        .map(vendor -> Money.clampScore(
                                Money.multiply(vendor.getRating(), new BigDecimal("20"))))
                        .orElse(Money.ZERO_MONEY));
    }

    private static BigDecimal asDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.longValue());
        }
        return BigDecimal.ZERO;
    }
}
