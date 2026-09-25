package com.tokenrealty.notification.kafka.in;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.kafka.KafkaJsonEvent;
import com.tokenrealty.notification.service.ProcessedEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class NotificationKafkaIngestSupport {

    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;

    public void consume(String message, String eventType, String failure, Consumer<KafkaJsonEvent> handler) {
        KafkaJsonEvent.consume(objectMapper, message, failure, event -> {
            if (!processedEventService.tryClaim(event.eventId(), eventType)) {
                return;
            }
            handler.accept(event);
        });
    }
}
