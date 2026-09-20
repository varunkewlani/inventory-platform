package com.inventoryplatform.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TransferInventoryRequest(
        @NotNull Long productId,
        @NotNull Long fromWarehouseId,
        @NotNull Long toWarehouseId,
        @Positive int quantity,
        @Size(max = 500) String note
) {
}
