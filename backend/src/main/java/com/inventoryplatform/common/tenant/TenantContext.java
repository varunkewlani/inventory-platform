package com.inventoryplatform.common.tenant;

import com.inventoryplatform.roles.Role;

/**
 * Per-request tenant/identity context, populated by {@link TenantFilter}
 * from the verified JWT (never from client-supplied input) and cleared at
 * the end of every request.
 *
 * <p>Service methods must read {@code organizationId} from here — not from
 * a controller parameter — so a tenant-scoped query can never be run with an
 * organization id that didn't come from the authenticated token.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> ORGANIZATION_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Role> ROLE = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long organizationId, Long userId, Role role) {
        ORGANIZATION_ID.set(organizationId);
        USER_ID.set(userId);
        ROLE.set(role);
    }

    public static Long getOrganizationId() {
        return ORGANIZATION_ID.get();
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static Role getRole() {
        return ROLE.get();
    }

    public static void clear() {
        ORGANIZATION_ID.remove();
        USER_ID.remove();
        ROLE.remove();
    }
}
