package com.vendorsphere.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import java.time.Instant;
import java.util.UUID;

/**
 * Mapped superclass for tables that carry {@code created_at} but no {@code updated_at} column.
 *
 * <p>Mirrors {@link BaseEntity} minus the update timestamp so that
 * {@code spring.jpa.hibernate.ddl-auto: validate} passes for the V1 tables that were created
 * without {@code updated_at}: {@code vendor_contacts}, {@code vendor_documents},
 * {@code purchase_request_items}, {@code rfq_items}, {@code rfq_vendors},
 * {@code quotation_items}, {@code vendor_evaluations}, {@code vendor_selections},
 * {@code purchase_order_items}, {@code deliveries}, {@code delivery_items},
 * {@code invoice_items}, {@code payments}, {@code vendor_performance_snapshots},
 * {@code notifications} and {@code audit_logs}.
 */
@MappedSuperclass
public abstract class CreatedOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
