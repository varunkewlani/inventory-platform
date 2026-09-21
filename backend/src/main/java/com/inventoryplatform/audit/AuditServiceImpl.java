package com.inventoryplatform.audit;

import com.inventoryplatform.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void log(String action, String entity, String entityId, Object oldValue, Object newValue) {
        log(TenantContext.getOrganizationId(), TenantContext.getUserId(), action, entity, entityId, oldValue, newValue);
    }

    @Override
    public void log(Long organizationId, Long userId, String action, String entity, String entityId, Object oldValue, Object newValue) {
        auditLogRepository.save(AuditLog.builder()
                .organizationId(organizationId)
                .userId(userId)
                .action(action)
                .entity(entity)
                .entityId(entityId)
                .oldValue(toJson(oldValue))
                .newValue(toJson(newValue))
                .build());
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            // Audit logging is best-effort for serialization failures
            // specifically — a bad toString() on some value shouldn't take
            // down the business operation being audited.
            log.warn("Failed to serialize audit log value", e);
            return null;
        }
    }
}
