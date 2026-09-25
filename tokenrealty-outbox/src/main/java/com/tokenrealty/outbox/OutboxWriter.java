package com.tokenrealty.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Base outbox writer — serializes {@link EventEnvelope} and persists via {@link #persistOutboxEvent}.
 * Each service provides a {@code @Service} subclass wired to its local {@code outbox_events} table.
 */
public abstract class OutboxWriter {

    private final ObjectMapper objectMapper;

    protected OutboxWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void enqueue(String eventType, String partitionKey, Object payload, String traceId) {
        enqueueWithEventId(eventType, partitionKey, payload, traceId, UUID.randomUUID());
    }

    @Transactional
    public void enqueueWithEventId(String eventType, String partitionKey, Object payload, String traceId,
                                   UUID eventId) {
        try {
            String json = objectMapper.writeValueAsString(
                    EventEnvelope.ofWithEventId(eventId, eventType, traceId, payload));
            persistOutboxEvent(eventType, partitionKey, json, traceId, eventId, Instant.now());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize outbox event", ex);
        }
    }

    protected abstract void persistOutboxEvent(String eventType, String partitionKey, String envelopeJson,
                                               String traceId, UUID eventId, Instant createdAt);
}
