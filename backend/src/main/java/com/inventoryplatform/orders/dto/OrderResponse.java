package com.inventoryplatform.orders.dto;

import com.inventoryplatform.orders.Order;
import com.inventoryplatform.orders.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long warehouseId,
        Long customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        String idempotencyKey,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(Order order, List<OrderItemResponse> items) {
        return new OrderResponse(
                order.getId(),
                order.getWarehouseId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getIdempotencyKey(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
