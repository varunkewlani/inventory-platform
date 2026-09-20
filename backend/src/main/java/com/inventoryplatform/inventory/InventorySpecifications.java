package com.inventoryplatform.inventory;

import org.springframework.data.jpa.domain.Specification;

public final class InventorySpecifications {

    private InventorySpecifications() {
    }

    public static Specification<Inventory> hasOrganizationId(Long organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId);
    }

    public static Specification<Inventory> hasWarehouseId(Long warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("warehouseId"), warehouseId);
    }

    public static Specification<Inventory> hasProductId(Long productId) {
        if (productId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("productId"), productId);
    }
}
