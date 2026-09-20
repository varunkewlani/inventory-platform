package com.inventoryplatform.inventory.dto;

import com.inventoryplatform.inventory.InventoryMovement;
import com.inventoryplatform.inventory.InventoryMovementType;

import java.time.Instant;

public record InventoryMovementResponse(
        Long id,
        InventoryMovementType movementType,
        int quantity,
        int availableQuantityAfter,
        int reservedQuantityAfter,
        String note,
        Long createdBy,
        Instant createdAt
) {
    public static InventoryMovementResponse from(InventoryMovement movement) {
        return new InventoryMovementResponse(
                movement.getId(),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getAvailableQuantityAfter(),
                movement.getReservedQuantityAfter(),
                movement.getNote(),
                movement.getCreatedBy(),
                movement.getCreatedAt()
        );
    }
}
