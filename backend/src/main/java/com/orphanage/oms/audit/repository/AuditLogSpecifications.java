package com.orphanage.oms.audit.repository;

import com.orphanage.oms.audit.entity.AuditLog;
import com.orphanage.oms.audit.enums.AuditAction;
import com.orphanage.oms.audit.enums.AuditModule;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Dynamic filters for audit log list queries.
 */
public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> build(
            String search,
            AuditModule module,
            AuditAction action,
            String username,
            UUID entityId,
            Instant from,
            Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("username")), pattern)));
            }
            if (module != null) {
                predicates.add(cb.equal(root.get("module"), module));
            }
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (username != null && !username.isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(root.get("username")),
                        username.trim().toLowerCase(Locale.ROOT)));
            }
            if (entityId != null) {
                predicates.add(cb.equal(root.get("entityId"), entityId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"), to));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
