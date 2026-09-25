package com.tokenrealty.notification.service;

import com.tokenrealty.events.kafka.KafkaJsonEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationLogService {

    public void logEvent(String eventType, KafkaJsonEvent event) {
        log.info("[NOTIFICATION STUB] type={} eventId={} payload={}",
                eventType, event.eventId(), event.payload());
    }
}
