package com.tokenrealty.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationSendService {

    private final NotificationEmailService notificationEmailService;

    public void sendManual(String eventType, JsonNode payload) {
        notificationEmailService.send(eventType, payload);
    }
}
