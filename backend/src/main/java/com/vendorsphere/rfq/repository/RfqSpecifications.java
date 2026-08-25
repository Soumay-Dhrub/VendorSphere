package com.vendorsphere.rfq.repository;

import com.vendorsphere.rfq.RfqStatus;
import com.vendorsphere.rfq.dto.RfqSearchCriteria;
import com.vendorsphere.rfq.entity.Rfq;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The predicate side of the RFQ listing (Requirement 31.1). The organization predicate is added first
 * and unconditionally; criteria contribute optional conjunctive filters only.
 */
public final class RfqSpecifications {

    private RfqSpecifications() {
        throw new AssertionError("No instances");
    }

    public static Specification<Rfq> search(UUID organizationId, RfqSearchCriteria criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("organization").get("id"), organizationId));

            if (criteria != null && criteria.status() != null) {
                predicates.add(builder.equal(root.get("status"), criteria.status()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
