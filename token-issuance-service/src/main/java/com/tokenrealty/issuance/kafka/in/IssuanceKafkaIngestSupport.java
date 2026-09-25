package com.tokenrealty.issuance.kafka.in;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.kafka.KafkaJsonEvent;
import com.tokenrealty.issuance.service.ProcessedEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class IssuanceKafkaIngestSupport {

    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;

    public void consume(String message, String eventType, String failure, Consumer<KafkaJsonEvent> handler) {
        KafkaJsonEvent.consume(objectMapper, message, failure, event -> {
            UUID eventId = event.eventId();
            if (!processedEventService.tryClaim(eventId, eventType)) {
                return;
            }
            handler.accept(event);
        });
    }
}
