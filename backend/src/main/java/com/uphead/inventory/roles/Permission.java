package com.uphead.inventory.roles;

/**
 * Granular permissions, per the assignment's bonus example
 * ({@code inventory.read}, {@code order.create}, ...). Roles map to a set of
 * these in {@link RolePermissions}; endpoints declare the permission they
 * need via {@link RequiresPermission}.
 */
public enum Permission {
    USER_MANAGE,
    USER_READ,
    PRODUCT_READ,
    PRODUCT_WRITE,
    WAREHOUSE_READ,
    WAREHOUSE_WRITE,
    INVENTORY_READ,
    INVENTORY_WRITE,
    ORDER_READ,
    ORDER_CREATE,
    ORDER_CANCEL,
    ORDER_UPDATE_STATUS,
    AUDIT_READ,
    REPORT_VIEW
}
