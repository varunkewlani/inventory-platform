package com.inventoryplatform.dashboard;

import com.inventoryplatform.common.response.ApiResponse;
import com.inventoryplatform.common.tenant.TenantContext;
import com.inventoryplatform.dashboard.dto.DashboardResponse;
import com.inventoryplatform.roles.Permission;
import com.inventoryplatform.roles.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @RequiresPermission(Permission.REPORT_VIEW)
    public ApiResponse<DashboardResponse> get() {
        return ApiResponse.success(dashboardService.get(TenantContext.getOrganizationId()));
    }
}
