package com.inventoryplatform.products.dto;

import com.inventoryplatform.products.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * All fields optional — PATCH semantics, only non-null fields are applied.
 * "Disable a product" is just this endpoint with {"status": "DISABLED"}.
 */
public record UpdateProductRequest(
        @Size(max = 100) String sku,
        @Size(max = 255) String name,
        @Size(max = 2000) String description,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal price,
        ProductStatus status
) {
}
