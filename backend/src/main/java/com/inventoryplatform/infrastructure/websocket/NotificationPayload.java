package com.inventoryplatform.infrastructure.websocket;

import java.time.Instant;

/** timestamp is a plain ISO-8601 string — see OrderEvent's javadoc for why Instant is avoided in payloads serialized outside Spring's own Jackson 3 config. */
public record NotificationPayload(String type, Object data, String timestamp) {

    public static NotificationPayload of(String type, Object data) {
        return new NotificationPayload(type, data, Instant.now().toString());
    }
}
