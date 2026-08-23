package com.vendorsphere.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * One reference number counter, keyed on organization, record type prefix and calendar year.
 *
 * <p>{@code next_value} holds the highest sequence value handed out so far and starts at {@code 0},
 * so the first allocation increments it to {@code 1} and formats as {@code 001}.
 *
 * <p>This entity deliberately extends neither {@link BaseEntity} nor {@link CreatedOnlyEntity}:
 * {@code reference_sequences} carries neither {@code created_at} nor {@code updated_at}, so either
 * superclass would map a column that does not exist and fail
 * {@code spring.jpa.hibernate.ddl-auto: validate}.
 *
 * <p>Rows are never written through this mapping. Allocation runs as a single upsert statement (see
 * {@code ReferenceSequenceRepository#allocateNextValue}) so that concurrent transactions serialize
 * on the row lock rather than racing a read-then-write.
 */
@Entity
@Table(name = "reference_sequences")
public class ReferenceSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "prefix", nullable = false, length = 10)
    private String prefix;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "next_value", nullable = false)
    private int nextValue;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getNextValue() {
        return nextValue;
    }

    public void setNextValue(int nextValue) {
        this.nextValue = nextValue;
    }
}
