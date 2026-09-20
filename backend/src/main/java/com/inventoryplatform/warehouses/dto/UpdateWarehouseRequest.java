package com.inventoryplatform.warehouses.dto;

import com.inventoryplatform.warehouses.WarehouseStatus;
import jakarta.validation.constraints.Size;

public record UpdateWarehouseRequest(
        @Size(max = 255) String name,
        @Size(max = 500) String address,
        WarehouseStatus status
) {
}
