package com.tokenrealty.kafka.consume;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.kafka.KafkaJsonEvent;
import com.tokenrealty.kafka.idempotency.ProcessedEventClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class KafkaEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessedEventClaimService processedEventClaimService;

    public void consume(String message, String eventType, String failure, Consumer<KafkaJsonEvent> handler) {
        KafkaJsonEvent.consume(objectMapper, message, failure, event -> {
            UUID eventId = event.eventId();
            if (!processedEventClaimService.tryClaim(eventId, eventType)) {
                return;
            }
            handler.accept(event);
        });
    }
}
