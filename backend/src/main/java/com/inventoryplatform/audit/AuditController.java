package com.inventoryplatform.audit;

import com.inventoryplatform.audit.dto.AuditLogResponse;
import com.inventoryplatform.common.response.ApiResponse;
import com.inventoryplatform.common.tenant.TenantContext;
import com.inventoryplatform.common.util.Specs;
import com.inventoryplatform.roles.Permission;
import com.inventoryplatform.roles.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @RequiresPermission(Permission.AUDIT_READ)
    public ApiResponse<Page<AuditLogResponse>> list(
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) String action,
            Pageable pageable) {
        Specification<AuditLog> spec = Specs.and(
                AuditSpecifications.hasOrganizationId(TenantContext.getOrganizationId()),
                AuditSpecifications.hasEntity(entity),
                AuditSpecifications.hasAction(action));

        Page<AuditLogResponse> page = auditLogRepository.findAll(spec, pageable).map(AuditLogResponse::from);
        return ApiResponse.success(page);
    }
}
