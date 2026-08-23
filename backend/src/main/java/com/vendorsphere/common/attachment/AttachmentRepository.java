package com.vendorsphere.common.attachment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Every finder is keyed on the organization, so a cross-tenant identifier misses and surfaces as
 * 404 rather than 403 (Requirement 30.10).
 */
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    Optional<Attachment> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<Attachment> findByOrganizationIdAndOwnerTypeAndOwnerIdOrderByCreatedAtAsc(
            UUID organizationId, AttachmentOwnerType ownerType, UUID ownerId);
}
