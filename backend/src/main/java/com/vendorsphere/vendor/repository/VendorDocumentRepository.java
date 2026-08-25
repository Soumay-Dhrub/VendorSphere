package com.vendorsphere.vendor.repository;

import com.vendorsphere.vendor.entity.VendorDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface VendorDocumentRepository extends JpaRepository<VendorDocument, UUID> {

    Optional<VendorDocument> findByIdAndVendorOrganizationId(UUID id, UUID organizationId);

    List<VendorDocument> findByVendorIdAndVendorOrganizationIdOrderByUploadedAtDesc(
            UUID vendorId, UUID organizationId);

    List<VendorDocument> findByExpiryDateIn(Collection<LocalDate> expiryDates);

    long countByVendorIdAndVendorOrganizationIdAndExpiryDateBetween(
            UUID vendorId, UUID organizationId, LocalDate from, LocalDate to);

    @Query("""
            SELECT d.vendor.id, COUNT(d)
            FROM VendorDocument d
            WHERE d.vendor.id IN :vendorIds
              AND d.vendor.organization.id = :organizationId
              AND d.expiryDate BETWEEN :from AND :to
            GROUP BY d.vendor.id
            """)
    List<Object[]> countExpiringDocumentsByVendorIds(
            @Param("vendorIds") Collection<UUID> vendorIds,
            @Param("organizationId") UUID organizationId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    default Map<UUID, Long> expiringDocumentCountsByVendorId(
            Collection<UUID> vendorIds, UUID organizationId, LocalDate from, LocalDate to) {
        if (vendorIds == null || vendorIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : countExpiringDocumentsByVendorIds(vendorIds, organizationId, from, to)) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }
}
