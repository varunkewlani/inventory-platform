package com.inventoryplatform.inventory.dto;

import java.time.Instant;

public record InventoryResponse(
        Long id,
        Long warehouseId,
        String warehouseName,
        Long productId,
        String productSku,
        String productName,
        int availableQuantity,
        int reservedQuantity,
        Instant updatedAt
) {
}
