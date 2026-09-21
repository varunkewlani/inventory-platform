package com.inventoryplatform.dashboard.dto;

import com.inventoryplatform.orders.dto.OrderResponse;

import java.util.List;

public record DashboardResponse(
        long totalProducts,
        long totalWarehouses,
        long availableInventory,
        long pendingOrders,
        long completedOrders,
        long lowStockProducts,
        List<OrderResponse> recentOrders
) {
}
