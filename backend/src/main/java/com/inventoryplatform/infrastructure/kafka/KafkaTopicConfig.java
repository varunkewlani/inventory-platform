package com.inventoryplatform.infrastructure.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String ORDER_EVENTS_TOPIC = "order-events";

    // A NewTopic bean makes Spring Kafka's KafkaAdmin provision it via
    // AdminClient at startup -- against an unreachable broker this blocked
    // Spring Boot startup for minutes regardless of AdminClient timeout
    // properties (KafkaAdmin retries its own topic-presence check
    // internally). Disabling the bean entirely when no broker is deployed
    // means there's nothing for KafkaAdmin to attempt in the first place.
    @Bean
    @ConditionalOnProperty(prefix = "app.kafka", name = "topic-provisioning-enabled", havingValue = "true", matchIfMissing = true)
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(ORDER_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
