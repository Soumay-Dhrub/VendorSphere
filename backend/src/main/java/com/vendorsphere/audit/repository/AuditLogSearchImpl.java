package com.vendorsphere.audit.repository;

import com.vendorsphere.audit.dto.AuditSearchCriteria;
import com.vendorsphere.audit.entity.AuditLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuditLogSearchImpl implements AuditLogSearch {

    private final EntityManager entityManager;

    public AuditLogSearchImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<AuditLog> search(
            UUID organizationId, AuditSearchCriteria criteria, Pageable pageable) {
        AuditSearchCriteria filters = criteria == null ? AuditSearchCriteria.none() : criteria;

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditLog> query = builder.createQuery(AuditLog.class);
        Root<AuditLog> root = query.from(AuditLog.class);
        query.where(predicates(builder, root, organizationId, filters));
        query.orderBy(QueryUtils.toOrders(pageable.getSort(), root, builder));

        TypedQuery<AuditLog> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        List<AuditLog> content = typedQuery.getResultList();

        // Skips the count query when the page alone already determines the total.
        return PageableExecutionUtils.getPage(
                content, pageable, () -> count(organizationId, filters));
    }

    private long count(UUID organizationId, AuditSearchCriteria filters) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<AuditLog> root = query.from(AuditLog.class);
        query.select(builder.count(root));
        query.where(predicates(builder, root, organizationId, filters));
        return entityManager.createQuery(query).getSingleResult();
    }

    private Predicate[] predicates(
            CriteriaBuilder builder,
            Root<AuditLog> root,
            UUID organizationId,
            AuditSearchCriteria filters) {
        List<Predicate> predicates = new ArrayList<>();

        // Requirements 29.3 and 30.10: reads never leave the caller's organization.
        predicates.add(builder.equal(root.get("organization").get("id"), organizationId));

        if (filters.actorId() != null) {
            predicates.add(builder.equal(root.get("actor").get("id"), filters.actorId()));
        }
        if (filters.entityType() != null && !filters.entityType().isBlank()) {
            predicates.add(builder.equal(root.get("entityType"), filters.entityType().trim()));
        }
        if (filters.entityId() != null) {
            predicates.add(builder.equal(root.get("entityId"), filters.entityId()));
        }
        // Requirement 29.6: the supplied range is inclusive at both ends.
        if (filters.from() != null) {
            predicates.add(builder.greaterThanOrEqualTo(
                    root.<Instant>get("createdAt"), filters.from()));
        }
        if (filters.to() != null) {
            predicates.add(builder.lessThanOrEqualTo(
                    root.<Instant>get("createdAt"), filters.to()));
        }

        return predicates.toArray(Predicate[]::new);
    }
}
