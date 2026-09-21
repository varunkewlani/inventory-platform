package com.inventoryplatform.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FulfillInventoryRequest(
        @NotNull Long warehouseId,
        @NotNull Long productId,
        @Positive int quantity
) {
}
