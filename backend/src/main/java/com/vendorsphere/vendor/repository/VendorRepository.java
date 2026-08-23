package com.vendorsphere.vendor.repository;

import com.vendorsphere.vendor.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Every finder is keyed on the organization, so a cross-tenant identifier misses and surfaces as
 * 404 rather than 403 (Requirement 30.10).
 *
 * <p>{@link JpaSpecificationExecutor} backs the filtered listing of Requirement 6, whose predicates
 * live in {@link VendorSpecifications}. The organization predicate is part of that specification, so
 * the inherited {@code findAll(Specification, Pageable)} is only ever called with it.
 */
public interface VendorRepository extends JpaRepository<Vendor, UUID>, JpaSpecificationExecutor<Vendor> {

    Optional<Vendor> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndEmailIgnoreCase(UUID organizationId, String email);

    /** Backs the delete guard of Requirement 4.6, which reports how many vendors use a category. */
    long countByOrganizationIdAndCategoryId(UUID organizationId, UUID categoryId);

    /**
     * The overall score of the vendor's most recent performance snapshot, empty when the performance
     * engine has not produced one yet. Backs the Performance_Score of Requirement 2.5.
     *
     * <p>A native projection rather than a mapped association: {@code vendor_performance_snapshots}
     * exists in V1 but its entity and repository belong to the performance module, so reading one
     * column here avoids a second mapping of the same table. Once that module lands the query can
     * move onto its repository unchanged.
     *
     * <p>The identifier is not organization-scoped because callers load the vendor tenant-scoped
     * first and pass its identifier, so a cross-tenant vendor never reaches this query.
     */
    @Query(value = """
            SELECT s.overall_score
            FROM vendor_performance_snapshots s
            WHERE s.vendor_id = :vendorId
            ORDER BY s.period_end DESC, s.calculated_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<BigDecimal> findLatestPerformanceScore(@Param("vendorId") UUID vendorId);

    /**
     * The latest snapshot score of every supplied vendor, as {@code [vendor_id, overall_score]} rows,
     * and the batch counterpart of {@link #findLatestPerformanceScore(UUID)}.
     *
     * <p>A list page carries a performance score per row, so calling the single-vendor query once per
     * row would issue one query per vendor and make the cost of a page grow with its size. This runs
     * once for the whole page instead (Requirement 31.2). {@code DISTINCT ON} keeps the first row of
     * each {@code vendor_id} group under the same ordering the single-vendor query uses, so both
     * report the same score. Vendors without a snapshot are simply absent from the result.
     *
     * <p>Prefer {@link #latestPerformanceScoresByVendorId(Collection)} over calling this directly.
     */
    @Query(value = """
            SELECT DISTINCT ON (s.vendor_id) s.vendor_id, s.overall_score
            FROM vendor_performance_snapshots s
            WHERE s.vendor_id IN (:vendorIds)
            ORDER BY s.vendor_id, s.period_end DESC, s.calculated_at DESC
            """, nativeQuery = true)
    List<Object[]> findLatestPerformanceScores(@Param("vendorIds") Collection<UUID> vendorIds);

    /**
     * {@link #findLatestPerformanceScores(Collection)} keyed by vendor identifier, with an empty input
     * short-circuited so an empty page issues no query at all.
     */
    default Map<UUID, BigDecimal> latestPerformanceScoresByVendorId(Collection<UUID> vendorIds) {
        if (vendorIds == null || vendorIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, BigDecimal> scores = new HashMap<>();
        for (Object[] row : findLatestPerformanceScores(vendorIds)) {
            scores.put((UUID) row[0], (BigDecimal) row[1]);
        }
        return scores;
    }
}
