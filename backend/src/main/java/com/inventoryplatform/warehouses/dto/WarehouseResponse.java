package com.inventoryplatform.warehouses.dto;

import com.inventoryplatform.warehouses.Warehouse;
import com.inventoryplatform.warehouses.WarehouseStatus;

import java.time.Instant;

public record WarehouseResponse(
        Long id,
        String name,
        String address,
        WarehouseStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.getAddress(),
                warehouse.getStatus(),
                warehouse.getCreatedAt(),
                warehouse.getUpdatedAt()
        );
    }
}
