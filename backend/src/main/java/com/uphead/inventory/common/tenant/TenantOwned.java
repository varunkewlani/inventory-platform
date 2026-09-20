package com.uphead.inventory.common.tenant;

/**
 * Marks an entity as belonging to a single organization. Purely
 * documentation/convention today — a visible signal that any repository
 * method touching this entity must take {@code organizationId} explicitly,
 * sourced from {@link TenantContext}, never from client input.
 */
public interface TenantOwned {
    Long getOrganizationId();
}
