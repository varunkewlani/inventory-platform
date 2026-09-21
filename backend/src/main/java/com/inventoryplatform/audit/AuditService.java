package com.inventoryplatform.audit;

/**
 * Append-only audit trail. The two-arg-prefix overload pulls
 * organization/user from {@link com.inventoryplatform.common.tenant.TenantContext}
 * for the common case (an authenticated user acting within their org); the
 * explicit-id overload exists for the one case where that context isn't
 * populated yet — login, which happens before a JWT (and therefore a
 * TenantContext) exists.
 */
public interface AuditService {

    void log(String action, String entity, String entityId, Object oldValue, Object newValue);

    void log(Long organizationId, Long userId, String action, String entity, String entityId, Object oldValue, Object newValue);
}
