package com.vendorsphere.vendor.repository;

import com.vendorsphere.vendor.dto.VendorSearchCriteria;
import com.vendorsphere.vendor.entity.Vendor;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class VendorSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private VendorSpecifications() {
        throw new AssertionError("No instances");
    }

    public static Specification<Vendor> search(UUID organizationId, VendorSearchCriteria criteria) {
        VendorSearchCriteria filters = criteria == null ? VendorSearchCriteria.none() : criteria;
        return (root, query, builder) -> {
            if (!isCountQuery(query)) {
                root.fetch("category", JoinType.LEFT);
            }
            return builder.and(predicates(root, builder, organizationId, filters));
        };
    }

    private static Predicate[] predicates(
            Root<Vendor> root,
            CriteriaBuilder builder,
            UUID organizationId,
            VendorSearchCriteria filters) {
        List<Predicate> predicates = new ArrayList<>();

        // Requirements 6.1 and 30.10: a listing never leaves the caller's organization.
        predicates.add(builder.equal(root.get("organization").get("id"), organizationId));

        // Requirement 6.2: case-insensitive contains, compared in lower case on both sides.
        String term = filters.companyNameTerm();
        if (term != null) {
            predicates.add(builder.like(
                    builder.lower(root.get("companyName")),
                    "%" + escapeLike(term.toLowerCase()) + "%",
                    LIKE_ESCAPE));
        }
        // Requirement 6.3.
        if (filters.categoryId() != null) {
            predicates.add(builder.equal(root.get("category").get("id"), filters.categoryId()));
        }
        // Requirement 6.4.
        if (filters.status() != null) {
            predicates.add(builder.equal(root.get("status"), filters.status()));
        }
        // Requirement 6.5: the bound is inclusive.
        if (filters.minRating() != null) {
            predicates.add(builder.greaterThanOrEqualTo(
                    root.<BigDecimal>get("rating"), filters.minRating()));
        }

        return predicates.toArray(Predicate[]::new);
    }

    private static String escapeLike(String term) {
        return term.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private static boolean isCountQuery(CriteriaQuery<?> query) {
        if (query == null) {
            return false;
        }
        Class<?> resultType = query.getResultType();
        return Long.class.equals(resultType) || long.class.equals(resultType);
    }
}
