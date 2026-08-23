package com.vendorsphere.vendor.repository;

import com.vendorsphere.vendor.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Every finder is keyed on the organization, so a cross-tenant identifier misses and surfaces as
 * 404 rather than 403 (Requirement 30.10).
 */
public interface VendorRepository extends JpaRepository<Vendor, UUID> {

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
}
