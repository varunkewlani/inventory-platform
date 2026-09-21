package com.inventoryplatform.infrastructure.kafka;

import com.inventoryplatform.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Its own consumer group ("audit-worker-group") — distinct from {@link
 * com.inventoryplatform.infrastructure.websocket.NotificationWorker}'s
 * group, so both independently receive every message on the topic rather
 * than splitting partitions between them.
 */
@Component
@RequiredArgsConstructor
public class AuditWorker {

    private final AuditService auditService;

    @KafkaListener(topics = KafkaTopicConfig.ORDER_EVENTS_TOPIC, groupId = "audit-worker-group")
    public void onOrderEvent(OrderEvent event) {
        Map<String, Object> oldValue = event.previousStatus() != null
                ? Map.of("status", event.previousStatus())
                : null;
        Map<String, Object> newValue = Map.of("status", event.newStatus());

        auditService.log(event.organizationId(), event.userId(), event.eventType(),
                "Order", String.valueOf(event.orderId()), oldValue, newValue);
    }
}
