package com.inventoryplatform.orders;

import com.inventoryplatform.orders.dto.CreateOrderRequest;
import com.inventoryplatform.orders.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponse create(CreateOrderRequest request, String idempotencyKey);

    OrderResponse getById(Long id);

    Page<OrderResponse> list(OrderStatus status, Long customerId, Pageable pageable);

    OrderResponse updateStatus(Long id, OrderStatus newStatus);
}
