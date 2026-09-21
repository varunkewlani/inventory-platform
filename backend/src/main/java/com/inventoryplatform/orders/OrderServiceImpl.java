package com.inventoryplatform.orders;

import com.inventoryplatform.common.exception.BadRequestException;
import com.inventoryplatform.common.exception.ConflictException;
import com.inventoryplatform.common.exception.ForbiddenException;
import com.inventoryplatform.common.exception.NotFoundException;
import com.inventoryplatform.common.tenant.TenantContext;
import com.inventoryplatform.common.util.Specs;
import com.inventoryplatform.customers.Customer;
import com.inventoryplatform.customers.CustomerRepository;
import com.inventoryplatform.inventory.InventoryService;
import com.inventoryplatform.inventory.dto.FulfillInventoryRequest;
import com.inventoryplatform.inventory.dto.ReleaseInventoryRequest;
import com.inventoryplatform.inventory.dto.ReserveInventoryRequest;
import com.inventoryplatform.orders.dto.*;
import com.inventoryplatform.products.Product;
import com.inventoryplatform.products.ProductRepository;
import com.inventoryplatform.products.ProductStatus;
import com.inventoryplatform.roles.Permission;
import com.inventoryplatform.roles.RolePermissions;
import com.inventoryplatform.warehouses.Warehouse;
import com.inventoryplatform.warehouses.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The order-creation flow, matching the spec's pipeline exactly: validate
 * user/org (already established by auth + TenantContext by the time we get
 * here) → validate products → check inventory → reserve inventory → create
 * order. Audit-event generation and the Kafka publish are wired in during
 * Phase 4, once AuditService/KafkaTemplate exist — that's a deliberate
 * split per plan.md, not an oversight.
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;

    @Override
    @Transactional
    public OrderResponse create(CreateOrderRequest request, String idempotencyKey) {
        Long organizationId = TenantContext.getOrganizationId();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = orderRepository.findByOrganizationIdAndIdempotencyKey(organizationId, idempotencyKey);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        Warehouse warehouse = warehouseRepository.findByIdAndOrganizationId(request.warehouseId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Warehouse not found"));
        Customer customer = customerRepository.findByIdAndOrganizationId(request.customerId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        // Validate every product up front (fail fast before reserving anything).
        Map<Long, Product> productsById = new HashMap<>();
        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productRepository.findByIdAndOrganizationId(itemRequest.productId(), organizationId)
                    .orElseThrow(() -> new NotFoundException("Product not found: " + itemRequest.productId()));
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new BadRequestException("Product is not available for order: " + product.getSku());
            }
            productsById.put(product.getId(), product);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productsById.get(itemRequest.productId());
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }

        // Reserve every line. Each call is its own atomic conditional UPDATE
        // (Phase 2); if any one fails, the InsufficientInventoryException
        // propagates and this whole @Transactional method rolls back —
        // including any reservations already made for earlier items in this
        // same loop. No manual compensation needed.
        for (OrderItemRequest itemRequest : request.items()) {
            inventoryService.reserve(new ReserveInventoryRequest(warehouse.getId(), itemRequest.productId(), itemRequest.quantity()));
        }

        Order order = orderRepository.save(Order.builder()
                .organizationId(organizationId)
                .warehouseId(warehouse.getId())
                .customerId(customer.getId())
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .idempotencyKey(idempotencyKey)
                .createdBy(TenantContext.getUserId())
                .build());

        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productsById.get(itemRequest.productId());
            items.add(orderItemRepository.save(OrderItem.builder()
                    .orderId(order.getId())
                    .productId(product.getId())
                    .quantity(itemRequest.quantity())
                    .unitPrice(product.getPrice())
                    .build()));
        }

        return toResponse(order, items, productsById);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        return toResponse(findTenantScoped(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> list(OrderStatus status, Long customerId, Pageable pageable) {
        Long organizationId = TenantContext.getOrganizationId();
        Specification<Order> spec = Specs.and(
                OrderSpecifications.hasOrganizationId(organizationId),
                OrderSpecifications.hasStatus(status),
                OrderSpecifications.hasCustomerId(customerId));

        return orderRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
        Order order = findTenantScoped(id);

        if (!OrderStatusTransitions.isAllowed(order.getStatus(), newStatus)) {
            throw new ConflictException("INVALID_STATUS_TRANSITION",
                    "Cannot transition order from " + order.getStatus() + " to " + newStatus);
        }

        // ORDER_UPDATE_STATUS (checked at the controller via @RequiresPermission)
        // covers ordinary progression; cancellation needs the narrower
        // ORDER_CANCEL permission, which STAFF doesn't have. The annotation
        // can't see the request body, so this one case is checked here instead.
        if (newStatus == OrderStatus.CANCELLED && !RolePermissions.has(TenantContext.getRole(), Permission.ORDER_CANCEL)) {
            throw new ForbiddenException("Role " + TenantContext.getRole() + " does not have permission ORDER_CANCEL");
        }

        List<OrderItem> items = orderItemRepository.findAllByOrderId(order.getId());

        if (newStatus == OrderStatus.CANCELLED) {
            for (OrderItem item : items) {
                inventoryService.release(new ReleaseInventoryRequest(order.getWarehouseId(), item.getProductId(), item.getQuantity()));
            }
        } else if (newStatus == OrderStatus.COMPLETED) {
            for (OrderItem item : items) {
                inventoryService.fulfill(new FulfillInventoryRequest(order.getWarehouseId(), item.getProductId(), item.getQuantity()));
            }
        }

        order.setStatus(newStatus);
        order = orderRepository.save(order);

        return toResponse(order, items, null);
    }

    private Order findTenantScoped(Long id) {
        return orderRepository.findByIdAndOrganizationId(id, TenantContext.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Order not found"));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItem> items = orderItemRepository.findAllByOrderId(order.getId());
        return toResponse(order, items, null);
    }

    private OrderResponse toResponse(Order order, List<OrderItem> items, Map<Long, Product> knownProducts) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItem item : items) {
            Product product = knownProducts != null ? knownProducts.get(item.getProductId()) : null;
            if (product == null) {
                product = productRepository.findById(item.getProductId()).orElse(null);
            }
            itemResponses.add(OrderItemResponse.from(item,
                    product != null ? product.getSku() : null,
                    product != null ? product.getName() : null));
        }
        return OrderResponse.from(order, itemResponses);
    }
}
