package com.inventoryplatform.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * {@code KafkaTemplate.send()} knows nothing about JPA transactions — if
 * {@code OrderServiceImpl} called it directly mid-transaction and the
 * transaction later rolled back, a Kafka message would go out for an order
 * that never actually got created. Instead, services publish {@link
 * OrderEvent} as an ordinary Spring {@code ApplicationEvent} (via {@code
 * ApplicationEventPublisher}, from inside the same transaction); this
 * listener only fires {@code AFTER_COMMIT}, so a Kafka message is only ever
 * sent for a change that's actually durable.
 */
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    // Disabling listener auto-startup/topic-provisioning (see KafkaTopicConfig,
    // application.yml) doesn't stop this producer from still trying to send --
    // and a send against no real broker doesn't fail fast, it burns a full
    // ~60s per call on Kafka's own background I/O thread waiting on metadata.
    // Two order-creating tests in the same run meant ~120s of avoidable
    // background load on a shared CI runner, which was very likely what
    // starved MySQL's connection pool for the next test class.
    @Value("${app.kafka.publishing-enabled:true}")
    private boolean publishingEnabled;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderEvent(OrderEvent event) {
        if (!publishingEnabled) {
            return;
        }
        kafkaTemplate.send(KafkaTopicConfig.ORDER_EVENTS_TOPIC, event.organizationId().toString(), event);
    }
}
