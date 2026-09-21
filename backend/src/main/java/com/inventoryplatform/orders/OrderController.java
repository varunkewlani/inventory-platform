package com.inventoryplatform.orders;

import com.inventoryplatform.common.response.ApiResponse;
import com.inventoryplatform.orders.dto.CreateOrderRequest;
import com.inventoryplatform.orders.dto.OrderResponse;
import com.inventoryplatform.orders.dto.UpdateOrderStatusRequest;
import com.inventoryplatform.roles.Permission;
import com.inventoryplatform.roles.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @RequiresPermission(Permission.ORDER_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> create(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResponse.success(orderService.create(request, idempotencyKey));
    }

    @GetMapping
    @RequiresPermission(Permission.ORDER_READ)
    public ApiResponse<Page<OrderResponse>> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long customerId,
            Pageable pageable) {
        return ApiResponse.success(orderService.list(status, customerId, pageable));
    }

    @GetMapping("/{id}")
    @RequiresPermission(Permission.ORDER_READ)
    public ApiResponse<OrderResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(orderService.getById(id));
    }

    @PatchMapping("/{id}/status")
    @RequiresPermission(Permission.ORDER_UPDATE_STATUS)
    public ApiResponse<OrderResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ApiResponse.success(orderService.updateStatus(id, request.status()));
    }
}
