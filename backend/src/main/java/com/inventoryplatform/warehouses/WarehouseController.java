package com.inventoryplatform.warehouses;

import com.inventoryplatform.common.response.ApiResponse;
import com.inventoryplatform.roles.Permission;
import com.inventoryplatform.roles.RequiresPermission;
import com.inventoryplatform.warehouses.dto.CreateWarehouseRequest;
import com.inventoryplatform.warehouses.dto.UpdateWarehouseRequest;
import com.inventoryplatform.warehouses.dto.WarehouseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping
    @RequiresPermission(Permission.WAREHOUSE_WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WarehouseResponse> create(@Valid @RequestBody CreateWarehouseRequest request) {
        return ApiResponse.success(warehouseService.create(request));
    }

    @GetMapping
    @RequiresPermission(Permission.WAREHOUSE_READ)
    public ApiResponse<Page<WarehouseResponse>> list(Pageable pageable) {
        return ApiResponse.success(warehouseService.list(pageable));
    }

    @GetMapping("/{id}")
    @RequiresPermission(Permission.WAREHOUSE_READ)
    public ApiResponse<WarehouseResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(warehouseService.getById(id));
    }

    @PatchMapping("/{id}")
    @RequiresPermission(Permission.WAREHOUSE_WRITE)
    public ApiResponse<WarehouseResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateWarehouseRequest request) {
        return ApiResponse.success(warehouseService.update(id, request));
    }
}
