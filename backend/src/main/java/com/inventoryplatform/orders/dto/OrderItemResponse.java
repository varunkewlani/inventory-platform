package com.inventoryplatform.orders.dto;

import com.inventoryplatform.orders.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
    public static OrderItemResponse from(OrderItem item, String productSku, String productName) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                productSku,
                productName,
                item.getQuantity(),
                item.getUnitPrice(),
                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
        );
    }
}
