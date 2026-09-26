package com.tokenrealty.marketplace.kafka.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import com.tokenrealty.outbox.OutboxStatus;
@Service
public class OutboxWriter extends com.tokenrealty.outbox.OutboxWriter {

    private static final String AGGREGATE_TYPE = "marketplace";

    private final OutboxEventRepository repository;

    @Value("${tokenrealty.kafka.enabled:false}")
    private boolean kafkaEnabled;

    public OutboxWriter(OutboxEventRepository repository, ObjectMapper objectMapper) {
        super(objectMapper);
        this.repository = repository;
    }

    @Override
    @Transactional
    public void enqueue(String eventType, String partitionKey, Object payload, String traceId) {
        if (!kafkaEnabled) {
            return;
        }
        super.enqueue(eventType, partitionKey, payload, traceId);
    }

    @Override
    @Transactional
    public void enqueueWithEventId(String eventType, String partitionKey, Object payload, String traceId,
                                   UUID eventId) {
        if (!kafkaEnabled) {
            return;
        }
        super.enqueueWithEventId(eventType, partitionKey, payload, traceId, eventId);
    }

    @Override
    protected void persistOutboxEvent(String eventType, String partitionKey, String envelopeJson,
                                      String traceId, UUID eventId, Instant createdAt) {
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(UUID.fromString(partitionKey))
                .eventType(eventType)
                .payload(envelopeJson)
                .status(OutboxStatus.PENDING)
                .createdAt(createdAt)
                .retryCount(0)
                .traceId(traceId)
                .build();
        repository.save(event);
    }
}
