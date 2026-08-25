package com.vendorsphere.invoice.repository;

import com.vendorsphere.invoice.entity.MatchFinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Match finding reads; re-evaluation deletes and recreates the invoice's findings. */
public interface MatchFindingRepository extends JpaRepository<MatchFinding, UUID> {

    List<MatchFinding> findByInvoiceIdOrderByCreatedAtAscIdAsc(UUID invoiceId);

    void deleteByInvoiceId(UUID invoiceId);
}
