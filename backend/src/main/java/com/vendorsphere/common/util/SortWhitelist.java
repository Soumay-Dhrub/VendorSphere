package com.vendorsphere.common.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable set of sortable field names for one list endpoint, together with the field
 * applied when a request omits {@code sort}.
 *
 * <p>Each controller declares its own whitelist so that {@link PageSupport#pageable} can
 * reject an unknown {@code sort} value with a message naming the accepted fields
 * (Requirement 31.5).
 */
public final class SortWhitelist {

    /** Sortable fields in declaration order, so error messages read predictably. */
    private final Set<String> fields;

    private final String defaultField;

    private SortWhitelist(Set<String> fields, String defaultField) {
        this.fields = Collections.unmodifiableSet(new LinkedHashSet<>(fields));
        this.defaultField = defaultField;
    }

    /**
     * Creates a whitelist whose first field is also the default sort field.
     *
     * @param defaultField the field applied when a request omits {@code sort}
     * @param otherFields  the remaining sortable fields
     */
    public static SortWhitelist of(String defaultField, String... otherFields) {
        Objects.requireNonNull(defaultField, "defaultField");
        Set<String> all = new LinkedHashSet<>();
        all.add(defaultField);
        for (String field : otherFields) {
            all.add(Objects.requireNonNull(field, "sortable field"));
        }
        return new SortWhitelist(all, defaultField);
    }

    /**
     * Creates a whitelist from an explicit collection of fields.
     *
     * @param fields       the sortable fields, which must contain {@code defaultField}
     * @param defaultField the field applied when a request omits {@code sort}
     */
    public static SortWhitelist of(Collection<String> fields, String defaultField) {
        Objects.requireNonNull(fields, "fields");
        Objects.requireNonNull(defaultField, "defaultField");
        Set<String> all = new LinkedHashSet<>(fields);
        if (!all.contains(defaultField)) {
            throw new IllegalArgumentException("Default sort field must be whitelisted: " + defaultField);
        }
        return new SortWhitelist(all, defaultField);
    }

    /** The field applied when a request omits {@code sort}. */
    public String defaultField() {
        return defaultField;
    }

    /** The sortable fields in declaration order. */
    public Set<String> fields() {
        return fields;
    }

    /** Whether {@code field} may be used as a sort field. */
    public boolean permits(String field) {
        return field != null && fields.contains(field);
    }

    /** The sortable fields rendered for an error message, in declaration order. */
    public String describe() {
        return String.join(", ", fields);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SortWhitelist that)) {
            return false;
        }
        return fields.equals(that.fields) && defaultField.equals(that.defaultField);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fields, defaultField);
    }

    @Override
    public String toString() {
        return "SortWhitelist[fields=" + describe() + ", default=" + defaultField + "]";
    }
}
