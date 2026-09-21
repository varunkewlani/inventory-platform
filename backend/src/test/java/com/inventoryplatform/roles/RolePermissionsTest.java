package com.inventoryplatform.roles;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RolePermissionsTest {

    @Test
    void adminHasEveryPermission() {
        for (Permission permission : Permission.values()) {
            assertThat(RolePermissions.has(Role.ADMIN, permission)).isTrue();
        }
    }

    @Test
    void managerCanWriteInventoryButStaffCannot() {
        assertThat(RolePermissions.has(Role.MANAGER, Permission.INVENTORY_WRITE)).isTrue();
        assertThat(RolePermissions.has(Role.STAFF, Permission.INVENTORY_WRITE)).isFalse();
    }

    @Test
    void staffCanReadInventoryAndCreateOrdersButNotCancelThem() {
        assertThat(RolePermissions.has(Role.STAFF, Permission.INVENTORY_READ)).isTrue();
        assertThat(RolePermissions.has(Role.STAFF, Permission.ORDER_CREATE)).isTrue();
        assertThat(RolePermissions.has(Role.STAFF, Permission.ORDER_UPDATE_STATUS)).isTrue();
        assertThat(RolePermissions.has(Role.STAFF, Permission.ORDER_CANCEL)).isFalse();
    }

    @Test
    void managerCanCancelOrders() {
        assertThat(RolePermissions.has(Role.MANAGER, Permission.ORDER_CANCEL)).isTrue();
    }

    @Test
    void onlyAdminCanManageUsersOrReadAuditLogs() {
        assertThat(RolePermissions.has(Role.ADMIN, Permission.USER_MANAGE)).isTrue();
        assertThat(RolePermissions.has(Role.MANAGER, Permission.USER_MANAGE)).isFalse();
        assertThat(RolePermissions.has(Role.STAFF, Permission.USER_MANAGE)).isFalse();

        assertThat(RolePermissions.has(Role.ADMIN, Permission.AUDIT_READ)).isTrue();
        assertThat(RolePermissions.has(Role.MANAGER, Permission.AUDIT_READ)).isFalse();
        assertThat(RolePermissions.has(Role.STAFF, Permission.AUDIT_READ)).isFalse();
    }
}
