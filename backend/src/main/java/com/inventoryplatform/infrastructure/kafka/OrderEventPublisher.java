package com.inventoryplatform.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderEvent(OrderEvent event) {
        kafkaTemplate.send(KafkaTopicConfig.ORDER_EVENTS_TOPIC, event.organizationId().toString(), event);
    }
}
