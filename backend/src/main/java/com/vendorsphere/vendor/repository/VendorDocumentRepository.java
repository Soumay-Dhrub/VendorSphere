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

/**
 * Documents hold no organization column of their own, so every finder traverses
 * {@code vendor.organization.id}. A cross-tenant identifier therefore misses and surfaces as 404
 * rather than 403 (Requirement 30.10).
 */
public interface VendorDocumentRepository extends JpaRepository<VendorDocument, UUID> {

    Optional<VendorDocument> findByIdAndVendorOrganizationId(UUID id, UUID organizationId);

    List<VendorDocument> findByVendorIdAndVendorOrganizationIdOrderByUploadedAtDesc(
            UUID vendorId, UUID organizationId);

    /**
     * Counts the vendor's documents whose expiry date falls inside the inclusive window
     * {@code [from, to]}, backing the expiring-document count of Requirement 2.5.
     *
     * <p>Callers pass the request date and the request date plus 30 days, which is the same window
     * the {@code EXPIRING_SOON} state of Requirement 5.4 describes; the document expiry evaluator
     * owns that classification, and this count stays consistent with it. A document without an
     * expiry date is never counted, because {@code BETWEEN} does not match {@code NULL}.
     */
    long countByVendorIdAndVendorOrganizationIdAndExpiryDateBetween(
            UUID vendorId, UUID organizationId, LocalDate from, LocalDate to);

    /**
     * The same count as
     * {@link #countByVendorIdAndVendorOrganizationIdAndExpiryDateBetween(UUID, UUID, LocalDate,
     * LocalDate)} for a whole set of vendors at once, as {@code [vendor_id, count]} rows.
     *
     * <p>A vendor list page carries the expiring-document count per row, so counting per row would
     * issue one query per vendor and make the cost of a page grow with its size. This aggregates the
     * whole page in one grouped query instead (Requirement 31.2). Vendors with no document in the
     * window produce no row, hence the zero default in
     * {@link #expiringDocumentCountsByVendorId(Collection, UUID, LocalDate, LocalDate)}.
     */
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

    /**
     * {@link #countExpiringDocumentsByVendorIds} keyed by vendor identifier, with an empty input
     * short-circuited so an empty page issues no query at all.
     */
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
