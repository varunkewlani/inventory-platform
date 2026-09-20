package com.inventoryplatform.inventory.dto;

import com.inventoryplatform.inventory.AdjustmentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdjustInventoryRequest(
        @NotNull Long warehouseId,
        @NotNull Long productId,
        @NotNull AdjustmentType type,
        @Positive int quantity,
        @Size(max = 500) String note
) {
}
