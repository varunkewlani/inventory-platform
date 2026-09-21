package com.inventoryplatform.audit;

import org.springframework.data.jpa.domain.Specification;

public final class AuditSpecifications {

    private AuditSpecifications() {
    }

    public static Specification<AuditLog> hasOrganizationId(Long organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId);
    }

    public static Specification<AuditLog> hasEntity(String entity) {
        if (entity == null || entity.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("entity"), entity);
    }

    public static Specification<AuditLog> hasAction(String action) {
        if (action == null || action.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("action"), action);
    }
}
