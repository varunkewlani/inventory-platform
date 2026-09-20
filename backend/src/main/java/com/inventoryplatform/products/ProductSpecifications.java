package com.inventoryplatform.products;

import org.springframework.data.jpa.domain.Specification;

/**
 * Predicate builders for GET /products?search=&status=. The organization
 * predicate is always applied by the service layer alongside whichever of
 * these are non-null — never optional, never left to the caller.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> hasOrganizationId(Long organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId);
    }

    public static Specification<Product> search(String term) {
        if (term == null || term.isBlank()) {
            return null;
        }
        String pattern = "%" + term.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("sku")), pattern));
    }

    public static Specification<Product> hasStatus(ProductStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
