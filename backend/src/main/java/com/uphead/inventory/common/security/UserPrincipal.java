package com.uphead.inventory.common.security;

import com.uphead.inventory.roles.Role;

/**
 * The authenticated identity attached to the Spring Security context by
 * {@link JwtAuthFilter}. Carries exactly what {@link
 * com.uphead.inventory.common.tenant.TenantFilter} and {@link
 * com.uphead.inventory.roles.PermissionAspect} need — nothing here is ever
 * populated from client input, only from a verified JWT's claims.
 */
public record UserPrincipal(Long userId, Long organizationId, String email, Role role) {
}
