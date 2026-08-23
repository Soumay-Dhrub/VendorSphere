package com.vendorsphere.notification.repository;

import com.vendorsphere.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Every finder is keyed on the recipient, so a notification addressed to another user - including a
 * user of another organization - simply misses and surfaces as 404 rather than 403
 * (Requirements 28.3 and 28.6).
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationInsert {

    Page<Notification> findByUserId(UUID userId, Pageable pageable);

    /** Requirement 28.4: the unread filter. */
    Page<Notification> findByUserIdAndReadFalse(UUID userId, Pageable pageable);

    /** Requirement 28.6: ownership is part of the lookup, not a check after the fact. */
    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    /** Requirement 28.8. */
    long countByUserIdAndReadFalse(UUID userId);

    /**
     * Requirement 28.7: one statement marks every unread notification of the user read, so the
     * result does not depend on how many rows happen to be loaded.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    int markAllReadForUser(@Param("userId") UUID userId);
}
