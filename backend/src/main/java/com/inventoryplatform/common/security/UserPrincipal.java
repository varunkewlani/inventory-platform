package com.inventoryplatform.common.security;

import com.inventoryplatform.roles.Role;

/**
 * The authenticated identity attached to the Spring Security context by
 * {@link JwtAuthFilter}. Carries exactly what {@link
 * com.inventoryplatform.common.tenant.TenantFilter} and {@link
 * com.inventoryplatform.roles.PermissionAspect} need — nothing here is ever
 * populated from client input, only from a verified JWT's claims.
 */
public record UserPrincipal(Long userId, Long organizationId, String email, Role role) {
}
