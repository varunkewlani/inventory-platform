package com.inventoryplatform.warehouses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWarehouseRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 500) String address
) {
}
