package com.vendorsphere.procurement.repository;

import com.vendorsphere.procurement.dto.PurchaseRequestSearchCriteria;
import com.vendorsphere.procurement.entity.PurchaseRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The predicate side of the purchase request listing (Requirements 6.1 analog, 31.1).
 *
 * <p>Every filter is optional and conjunctive: a supplied value adds one predicate, an absent one
 * adds none. The organization predicate is added first and unconditionally, before any criteria are
 * inspected - the criteria record carries no organization component at all, so the only way to change
 * tenant is to pass a different {@code organizationId}, which only the service does and only from the
 * security context (Requirement 30.10).
 *
 * <p>The same is true of the requester predicate: {@code restrictedRequesterId} arrives from the
 * service's visibility rule, never from a query parameter, so a caller cannot widen its own scope by
 * omitting it.
 */
public final class PurchaseRequestSpecifications {

    private PurchaseRequestSpecifications() {
        throw new AssertionError("No instances");
    }

    /**
     * A listing of the given organization's requests narrowed by the supplied filters.
     *
     * @param organizationId         the caller's organization, never {@code null}
     * @param criteria               the optional filters, {@code null} meaning unfiltered
     * @param restrictedRequesterId  when non-null, only requests authored by this user; used to narrow
     *                               REQUESTER-only callers to their own requests (Requirement 8.9)
     */
    public static Specification<PurchaseRequest> search(
            UUID organizationId,
            PurchaseRequestSearchCriteria criteria,
            UUID restrictedRequesterId) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Tenant scope first and always (Requirement 30.10).
            predicates.add(builder.equal(root.get("organization").get("id"), organizationId));

            if (restrictedRequesterId != null) {
                predicates.add(builder.equal(root.get("requester").get("id"), restrictedRequesterId));
            }
            PurchaseRequestSearchCriteria filters =
                    criteria == null ? PurchaseRequestSearchCriteria.none() : criteria;
            if (filters.status() != null) {
                predicates.add(builder.equal(root.get("status"), filters.status()));
            }
            if (filters.departmentId() != null) {
                predicates.add(builder.equal(root.get("department").get("id"), filters.departmentId()));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
