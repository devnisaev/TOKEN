package com.tokenrealty.notification.service;

import com.tokenrealty.events.kafka.KafkaJsonEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationLogService {

    private final NotificationEmailService notificationEmailService;

    public void logEvent(String eventType, KafkaJsonEvent event) {
        log.info("[NOTIFICATION] type={} eventId={} payload={}",
                eventType, event.eventId(), event.payload());
        notificationEmailService.send(eventType, event.payload());
    }
}
