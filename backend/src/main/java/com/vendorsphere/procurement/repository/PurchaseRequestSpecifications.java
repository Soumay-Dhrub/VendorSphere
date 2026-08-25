package com.vendorsphere.procurement.repository;

import com.vendorsphere.procurement.dto.PurchaseRequestSearchCriteria;
import com.vendorsphere.procurement.entity.PurchaseRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PurchaseRequestSpecifications {

    private PurchaseRequestSpecifications() {
        throw new AssertionError("No instances");
    }

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
