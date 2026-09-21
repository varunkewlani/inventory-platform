package com.inventoryplatform.infrastructure.websocket;

import com.inventoryplatform.infrastructure.kafka.KafkaTopicConfig;
import com.inventoryplatform.infrastructure.kafka.OrderEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Own consumer group ("notification-worker-group") — see {@link com.inventoryplatform.infrastructure.kafka.AuditWorker} for why. */
@Component
@RequiredArgsConstructor
public class NotificationWorker {

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopicConfig.ORDER_EVENTS_TOPIC, groupId = "notification-worker-group")
    public void onOrderEvent(OrderEvent event) {
        notificationService.notifyOrganization(event.organizationId(), event.eventType(), Map.of(
                "orderId", event.orderId(),
                "status", event.newStatus()
        ));
    }
}
