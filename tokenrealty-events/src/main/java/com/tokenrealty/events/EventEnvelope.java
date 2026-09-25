package com.tokenrealty.events;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String traceId,
        T payload
) {
    public static <T> EventEnvelope<T> of(String eventType, String traceId, T payload) {
        return new EventEnvelope<>(UUID.randomUUID(), eventType, Instant.now(), traceId, payload);
    }

    public static <T> EventEnvelope<T> ofWithEventId(UUID eventId, String eventType, String traceId, T payload) {
        return new EventEnvelope<>(eventId, eventType, Instant.now(), traceId, payload);
    }
}
