package com.inventoryplatform.inventory;

import com.inventoryplatform.common.response.ApiResponse;
import com.inventoryplatform.inventory.dto.*;
import com.inventoryplatform.roles.Permission;
import com.inventoryplatform.roles.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @RequiresPermission(Permission.INVENTORY_READ)
    public ApiResponse<Page<InventoryResponse>> list(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,
            Pageable pageable) {
        return ApiResponse.success(inventoryService.list(warehouseId, productId, pageable));
    }

    @GetMapping("/{id}/history")
    @RequiresPermission(Permission.INVENTORY_READ)
    public ApiResponse<Page<InventoryMovementResponse>> history(@PathVariable Long id, Pageable pageable) {
        return ApiResponse.success(inventoryService.history(id, pageable));
    }

    @PostMapping("/adjust")
    @RequiresPermission(Permission.INVENTORY_WRITE)
    public ApiResponse<InventoryResponse> adjust(@Valid @RequestBody AdjustInventoryRequest request) {
        return ApiResponse.success(inventoryService.adjust(request));
    }

    @PostMapping("/transfer")
    @RequiresPermission(Permission.INVENTORY_WRITE)
    public ApiResponse<TransferResult> transfer(@Valid @RequestBody TransferInventoryRequest request) {
        return ApiResponse.success(inventoryService.transfer(request));
    }

    @PostMapping("/reserve")
    @RequiresPermission(Permission.INVENTORY_WRITE)
    public ApiResponse<InventoryResponse> reserve(@Valid @RequestBody ReserveInventoryRequest request) {
        return ApiResponse.success(inventoryService.reserve(request));
    }

    @PostMapping("/release")
    @RequiresPermission(Permission.INVENTORY_WRITE)
    public ApiResponse<InventoryResponse> release(@Valid @RequestBody ReleaseInventoryRequest request) {
        return ApiResponse.success(inventoryService.release(request));
    }

    @PostMapping("/fulfill")
    @RequiresPermission(Permission.INVENTORY_WRITE)
    public ApiResponse<InventoryResponse> fulfill(@Valid @RequestBody FulfillInventoryRequest request) {
        return ApiResponse.success(inventoryService.fulfill(request));
    }
}
