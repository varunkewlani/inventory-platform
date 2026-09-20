package com.inventoryplatform.roles;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Static role -> permission-set mapping. Per the spec's RBAC table: Admin
 * gets full organization access, Manager manages products/inventory/orders,
 * Staff reads inventory and processes orders.
 */
public final class RolePermissions {

    private static final Map<Role, Set<Permission>> MAP = new EnumMap<>(Role.class);

    static {
        MAP.put(Role.ADMIN, EnumSet.allOf(Permission.class));

        MAP.put(Role.MANAGER, EnumSet.of(
                Permission.USER_READ,
                Permission.PRODUCT_READ, Permission.PRODUCT_WRITE,
                Permission.WAREHOUSE_READ, Permission.WAREHOUSE_WRITE,
                Permission.INVENTORY_READ, Permission.INVENTORY_WRITE,
                Permission.ORDER_READ, Permission.ORDER_CREATE,
                Permission.ORDER_CANCEL, Permission.ORDER_UPDATE_STATUS,
                Permission.REPORT_VIEW
        ));

        MAP.put(Role.STAFF, EnumSet.of(
                Permission.PRODUCT_READ,
                Permission.WAREHOUSE_READ,
                Permission.INVENTORY_READ,
                Permission.ORDER_READ, Permission.ORDER_CREATE,
                Permission.ORDER_UPDATE_STATUS
        ));
    }

    private RolePermissions() {
    }

    public static boolean has(Role role, Permission permission) {
        return MAP.getOrDefault(role, Set.of()).contains(permission);
    }
}
