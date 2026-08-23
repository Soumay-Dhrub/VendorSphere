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

/**
 * The predicate side of the vendor listing (Requirement 6).
 *
 * <p>Every predicate is conjunctive and every filter is optional, which is what makes Requirement
 * 6.6 hold by construction: a supplied filter adds one predicate, an absent filter adds none, and
 * the result is the AND of whatever was supplied.
 */
public final class VendorSpecifications {

    /** Escape character for the {@code LIKE} term, so a term containing {@code %} matches literally. */
    private static final char LIKE_ESCAPE = '\\';

    private VendorSpecifications() {
        throw new AssertionError("No instances");
    }

    /**
     * A listing of the given organization's vendors narrowed by whichever criteria are supplied.
     *
     * <p>The organization predicate is added first and unconditionally, before any criteria are
     * inspected. It is therefore not expressible as, or defeatable by, a criteria value: the criteria
     * record carries no organization component at all, so the only way to change tenant is to pass a
     * different {@code organizationId}, which only the service does and only from the security
     * context (Requirements 6.1, 30.10).
     *
     * <p>The category association is left-join fetched on the content query so that projecting
     * {@code categoryName} for a page of vendors does not issue one lazy load per row. The fetch is
     * skipped on the count query Spring Data derives for the same specification, because a fetch join
     * has no meaning under {@code count()} and Hibernate rejects it there (Requirement 31.2).
     *
     * @param organizationId the caller's organization, never {@code null}
     * @param criteria       the optional filters, {@code null} meaning unfiltered
     */
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

    /**
     * Neutralizes the {@code LIKE} metacharacters in a search term, so a term such as {@code 50%} is
     * a search for the three characters rather than a wildcard. The escape character itself is
     * escaped first, otherwise a trailing backslash would escape the closing wildcard.
     */
    private static String escapeLike(String term) {
        return term.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /** Whether this is the count query Spring Data derives to compute {@code totalElements}. */
    private static boolean isCountQuery(CriteriaQuery<?> query) {
        if (query == null) {
            return false;
        }
        Class<?> resultType = query.getResultType();
        return Long.class.equals(resultType) || long.class.equals(resultType);
    }
}
