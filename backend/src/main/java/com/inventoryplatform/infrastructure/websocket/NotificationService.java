package com.inventoryplatform.infrastructure.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyOrganization(Long organizationId, String type, Object data) {
        messagingTemplate.convertAndSend(
                "/topic/organizations/" + organizationId,
                NotificationPayload.of(type, data));
    }
}
