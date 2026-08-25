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

public interface VendorRepository extends JpaRepository<Vendor, UUID>, JpaSpecificationExecutor<Vendor> {

    Optional<Vendor> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("""
            SELECT v.id FROM Vendor v
            WHERE v.user.id = :userId AND v.organization.id = :organizationId
            ORDER BY v.id
            """)
    List<UUID> findIdsByUserIdAndOrganizationId(
            @Param("userId") UUID userId, @Param("organizationId") UUID organizationId);

    boolean existsByOrganizationIdAndEmailIgnoreCase(UUID organizationId, String email);

    long countByOrganizationIdAndCategoryId(UUID organizationId, UUID categoryId);

    @Query(value = """
            SELECT s.overall_score
            FROM vendor_performance_snapshots s
            WHERE s.vendor_id = :vendorId
            ORDER BY s.period_end DESC, s.calculated_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<BigDecimal> findLatestPerformanceScore(@Param("vendorId") UUID vendorId);

    @Query(value = """
            SELECT DISTINCT ON (s.vendor_id) s.vendor_id, s.overall_score
            FROM vendor_performance_snapshots s
            WHERE s.vendor_id IN (:vendorIds)
            ORDER BY s.vendor_id, s.period_end DESC, s.calculated_at DESC
            """, nativeQuery = true)
    List<Object[]> findLatestPerformanceScores(@Param("vendorIds") Collection<UUID> vendorIds);

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
