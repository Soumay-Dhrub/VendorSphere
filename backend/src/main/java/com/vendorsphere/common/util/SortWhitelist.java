package com.vendorsphere.common.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class SortWhitelist {

    private final Set<String> fields;

    private final String defaultField;

    private SortWhitelist(Set<String> fields, String defaultField) {
        this.fields = Collections.unmodifiableSet(new LinkedHashSet<>(fields));
        this.defaultField = defaultField;
    }

    public static SortWhitelist of(String defaultField, String... otherFields) {
        Objects.requireNonNull(defaultField, "defaultField");
        Set<String> all = new LinkedHashSet<>();
        all.add(defaultField);
        for (String field : otherFields) {
            all.add(Objects.requireNonNull(field, "sortable field"));
        }
        return new SortWhitelist(all, defaultField);
    }

    public static SortWhitelist of(Collection<String> fields, String defaultField) {
        Objects.requireNonNull(fields, "fields");
        Objects.requireNonNull(defaultField, "defaultField");
        Set<String> all = new LinkedHashSet<>(fields);
        if (!all.contains(defaultField)) {
            throw new IllegalArgumentException("Default sort field must be whitelisted: " + defaultField);
        }
        return new SortWhitelist(all, defaultField);
    }

    public String defaultField() {
        return defaultField;
    }

    public Set<String> fields() {
        return fields;
    }

    public boolean permits(String field) {
        return field != null && fields.contains(field);
    }

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
