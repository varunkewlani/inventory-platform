package com.inventoryplatform.infrastructure.kafka;

import java.time.Instant;

/**
 * Published to the {@code order-events} topic whenever an order is created
 * or changes status. Two independent consumer groups read every message
 * (see {@link AuditWorker}, {@link com.inventoryplatform.infrastructure.websocket.NotificationWorker}) —
 * that's the whole point of the async split: if the notification path is
 * briefly down, the order already succeeded and its audit trail is written
 * independently.
 *
 * <p>{@code timestamp} is a plain ISO-8601 string, not {@link Instant} —
 * Spring Kafka's {@code JsonSerializer} carries its own bundled Jackson 2
 * {@code ObjectMapper} (separate from Spring's own Jackson 3 one) with no
 * JSR-310 module registered, so an {@code Instant} field fails to
 * serialize at runtime rather than at compile time.
 */
public record OrderEvent(
        String eventType,
        Long organizationId,
        Long orderId,
        Long userId,
        String previousStatus,
        String newStatus,
        String timestamp
) {
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_STATUS_CHANGED = "ORDER_STATUS_CHANGED";

    public static OrderEvent created(Long organizationId, Long orderId, Long userId, String status) {
        return new OrderEvent(ORDER_CREATED, organizationId, orderId, userId, null, status, Instant.now().toString());
    }

    public static OrderEvent statusChanged(Long organizationId, Long orderId, Long userId, String previousStatus, String newStatus) {
        return new OrderEvent(ORDER_STATUS_CHANGED, organizationId, orderId, userId, previousStatus, newStatus, Instant.now().toString());
    }
}
