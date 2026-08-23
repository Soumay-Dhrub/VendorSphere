package com.vendorsphere.notification.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Reads {@code vendors.user_id} directly.
 *
 * <p>A native query rather than a JPA finder because the {@code Vendor} entity does not exist yet -
 * it arrives with the vendor module - while the {@code vendors} table has existed since V1. The query
 * touches one column of one table, so it stays correct once the entity is mapped, and the vendor
 * module can replace this bean with an entity-based implementation without touching any caller.
 *
 * <p>Runs on the caller's connection and opens no transaction of its own.
 */
@Component
class NativeVendorUserDirectory implements VendorUserDirectory {

    private static final String USER_IDS_SQL =
            "SELECT user_id FROM vendors WHERE id = :vendorId AND user_id IS NOT NULL";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<UUID> findUserIdsOfVendor(UUID vendorId) {
        if (vendorId == null) {
            return List.of();
        }
        List<?> rows = entityManager
                .createNativeQuery(USER_IDS_SQL)
                .setParameter("vendorId", vendorId)
                .getResultList();
        return rows.stream().map(UUID.class::cast).toList();
    }
}
