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
    private final NotificationPreferenceGate preferenceGate;

    public void logEvent(String eventType, KafkaJsonEvent event) {
        log.info("[NOTIFICATION] type={} eventId={} payload={}",
                eventType, event.eventId(), event.payload());

        var userId = NotificationPayloadUsers.resolveUserId(event.payload());
        if (!preferenceGate.shouldDeliver(eventType, userId)) {
            log.info("[NOTIFICATION skipped] type={} userId={} (preferences)", eventType, userId);
            return;
        }

        notificationEmailService.send(eventType, event.payload());
    }
}
