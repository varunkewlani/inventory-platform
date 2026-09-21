package com.inventoryplatform.dashboard;

import com.inventoryplatform.dashboard.dto.DashboardResponse;
import com.inventoryplatform.inventory.InventoryRepository;
import com.inventoryplatform.orders.OrderRepository;
import com.inventoryplatform.orders.OrderService;
import com.inventoryplatform.orders.OrderStatus;
import com.inventoryplatform.products.ProductRepository;
import com.inventoryplatform.warehouses.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * Cache strategy: key {@code dashboard:{organizationId}}, TTL 30s, no
 * active invalidation on writes — for aggregate counts across five tables,
 * write-side invalidation would mean touching every mutating endpoint in
 * the app for a widget that's inherently a point-in-time snapshot anyway.
 * A 30s staleness window is an explicit, documented trade-off: acceptable
 * for a dashboard, not for anything transactional (which never goes
 * through this cache).
 *
 * <p>Cached as a plain JSON string (not via a typed RedisTemplate) —
 * Spring Data Redis's generic JSON serializer doesn't embed type metadata
 * the way it looks like it should, so reading back gives a raw
 * {@code LinkedHashMap}, not a {@code DashboardResponse}. Deserializing
 * explicitly with a known target type sidesteps that entirely.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Duration TTL = Duration.ofSeconds(30);
    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final int RECENT_ORDERS_LIMIT = 10;

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public DashboardResponse get(Long organizationId) {
        String key = "dashboard:" + organizationId;

        String cachedJson = redisTemplate.opsForValue().get(key);
        if (cachedJson != null) {
            return objectMapper.readValue(cachedJson, DashboardResponse.class);
        }

        DashboardResponse fresh = compute(organizationId);
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(fresh), TTL);
        return fresh;
    }

    private DashboardResponse compute(Long organizationId) {
        return new DashboardResponse(
                productRepository.countByOrganizationId(organizationId),
                warehouseRepository.countByOrganizationId(organizationId),
                inventoryRepository.sumAvailableQuantityByOrganizationId(organizationId),
                orderRepository.countByOrganizationIdAndStatus(organizationId, OrderStatus.PENDING),
                orderRepository.countByOrganizationIdAndStatus(organizationId, OrderStatus.COMPLETED),
                inventoryRepository.countByOrganizationIdAndAvailableQuantityLessThanEqual(organizationId, LOW_STOCK_THRESHOLD),
                orderService.getRecent(RECENT_ORDERS_LIMIT)
        );
    }
}
