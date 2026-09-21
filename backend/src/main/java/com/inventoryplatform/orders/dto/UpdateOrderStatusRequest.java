package com.inventoryplatform.orders.dto;

import com.inventoryplatform.orders.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
}
